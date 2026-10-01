package ru.sobes.dashboard

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.core.env.Environment
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import ru.sobes.auth.TestTelegramAuth
import ru.sobes.practice.Rating
import ru.sobes.practice.ReviewSchedule
import ru.sobes.practice.ReviewScheduleId
import ru.sobes.practice.ReviewScheduleRepository
import ru.sobes.practice.UserAnswer
import ru.sobes.practice.UserAnswerRepository
import ru.sobes.question.Category
import ru.sobes.question.Difficulty
import ru.sobes.question.Question
import ru.sobes.question.QuestionRepository
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Интеграционные тесты дашборда. Данные сеем напрямую в репозитории
 * (практику тестирует PracticeApiTest, тут — только агрегация).
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "sobes.telegram.bot-token=${TestTelegramAuth.BOT_TOKEN}",
        "sobes.jwt.secret=test-jwt-secret-at-least-32-characters-long",
    ],
)
@Testcontainers(disabledWithoutDocker = true)
class DashboardApiTest {

    @Autowired private lateinit var env: Environment
    @Autowired private lateinit var questions: QuestionRepository
    @Autowired private lateinit var answers: UserAnswerRepository
    @Autowired private lateinit var schedules: ReviewScheduleRepository

    private val mapper = jacksonObjectMapper()
    private val client = HttpClient.newHttpClient()

    private fun login(uid: Long): String {
        val port = requireNotNull(env.getProperty("local.server.port"))
        val body = mapper.writeValueAsString(TestTelegramAuth.signedWidgetParams(id = uid))
        val req = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/api/v1/auth/telegram"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        return mapper.readTree(client.send(req, HttpResponse.BodyHandlers.ofString()).body())["token"].asText()
    }

    private fun get(token: String, path: String): HttpResponse<String> {
        val port = requireNotNull(env.getProperty("local.server.port"))
        val req = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port$path"))
            .header("Authorization", "Bearer $token")
            .GET().build()
        return client.send(req, HttpResponse.BodyHandlers.ofString())
    }

    @Test
    fun `пустой дашборд возвращает нули и не падает`() {
        val token = login(600100L)
        val r = get(token, "/api/v1/dashboard")
        assertEquals(200, r.statusCode(), r.body())
        val d = mapper.readTree(r.body())
        assertEquals(0, d["answeredTotal"].asLong())
        assertEquals(0, d["streakDays"].asInt())
        assertEquals(0, d["readinessPercent"].asInt())
        assertEquals(0, d["weakestTopics"].size())
    }

    @Test
    fun `слабые темы и streak считаются по истории ответов`() {
        val uid = 600200L
        val token = login(uid)

        // две категории: сильная (EASY x2), слабая (AGAIN x2)
        val strongCat = questions.findAll().first { it.category.slug == "java-core" }
        val weakCat = questions.findAll().first { it.category.slug == "kotlin" }
        listOf(strongCat to Rating.EASY, strongCat to Rating.EASY).forEach { (q, r) ->
            answers.save(UserAnswer(userId = uid, questionId = q.id!!, selfRating = r.name))
        }
        listOf(weakCat to Rating.AGAIN, weakCat to Rating.AGAIN).forEach { (q, r) ->
            answers.save(UserAnswer(userId = uid, questionId = q.id!!, selfRating = r.name))
        }
        // started для готовности: 4 вопроса в расписании
        listOf(strongCat, weakCat).flatMap { listOf(it, it) }.forEachIndexed { i, q ->
            schedules.save(ReviewSchedule(userId = uid, questionId = q.id!!))
        }

        val d = mapper.readTree(get(token, "/api/v1/dashboard").body())
        // totalAnsweredConfidence не сериализуется? проверим поля
        assertEquals(4, d["answeredTotal"].asLong())
        assertTrue(d["streakDays"].asInt() >= 1, "ответы сегодня — streak >= 1, было ${d["streakDays"]}")

        val weak = d["weakestTopics"].firstOrNull { it["slug"].asText() == "kotlin" }
        assertTrue(weak != null, "kotlin должен быть в слабых темах: ${d["weakestTopics"]}")
        assertEquals(0, weak!!["confidencePercent"].asInt(), "AGAIN x2 = 0% уверенности")

        val strong = d["categoryProgress"].first { it["slug"].asText() == "java-core" }
        assertEquals(100, strong["confidencePercent"].asInt(), "EASY x2 = 100%")
        assertTrue(d["readinessPercent"].asInt() > 0, "готовность должна быть ненулевой")
    }

    @Test
    fun `дашборд без токена отдаёт 401`() {
        val port = requireNotNull(env.getProperty("local.server.port"))
        try {
            client.send(
                HttpRequest.newBuilder().uri(URI.create("http://localhost:$port/api/v1/dashboard")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
            )
        } catch (_: Exception) {}
        // ожидаем HTTPError 401
        val req = HttpRequest.newBuilder().uri(URI.create("http://localhost:$port/api/v1/dashboard")).GET().build()
        val resp = client.send(req, HttpResponse.BodyHandlers.ofString())
        assertEquals(401, resp.statusCode())
    }

    companion object {
        @JvmStatic
        @Container
        @ServiceConnection
        private val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
    }
}
