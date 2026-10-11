package ru.sobes.interview

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.core.env.Environment
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import ru.sobes.auth.TestTelegramAuth
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@SpringBootTest(
    properties = [
        "sobes.telegram.bot-token=${TestTelegramAuth.BOT_TOKEN}",
        "sobes.jwt.secret=test-jwt-secret-at-least-32-characters-long",
    ],
)
@Testcontainers(disabledWithoutDocker = true)
class InterviewApiTest {

    @Autowired private lateinit var env: Environment

    private val mapper = jacksonObjectMapper()
    private val client = HttpClient.newHttpClient()

    private fun port() = requireNotNull(env.getProperty("local.server.port"))

    private fun login(uid: Long): String {
        val body = mapper.writeValueAsString(TestTelegramAuth.signedWidgetParams(id = uid))
        val r = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:${port()}/api/v1/auth/telegram"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
            HttpResponse.BodyHandlers.ofString()
        )
        return mapper.readTree(r.body())["token"].asText()
    }

    private fun call(method: String, path: String, token: String, body: Any? = null): Pair<Int, JsonNode?> {
        val b = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:${port()}$path"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $token")
        when (method) {
            "GET" -> b.GET()
            else -> b.POST(HttpRequest.BodyPublishers.ofString(body?.let { mapper.writeValueAsString(it) } ?: ""))
        }
        val resp = client.send(b.build(), HttpResponse.BodyHandlers.ofString())
        val tree = runCatching { mapper.readTree(resp.body()) }.getOrNull()
        return resp.statusCode() to tree
    }

    @Test
    fun `полное интервью с отчётом и записью в SM2`() {
        val token = login(7733001L)

        // 1. старт: 5 вопросов из java-core
        val (c1, s1) = call("POST", "/api/v1/interview/start", token,
            mapOf("categories" to listOf("java-core"), "total" to 5))
        assert(c1 == 200) { "$c1 $s1" }
        val sessionId = s1!!["sessionId"].asLong()
        assert(s1["total"].asInt() == 5) { s1.toString() }
        assert(!s1["question"].isNull && s1["question"]["secondsLimit"].asInt() == 180)

        // 2. отвечаем на все 5
        val ratings = listOf("GOOD", "AGAIN", "GOOD", "EASY", "HARD")
        var state = s1
        for (rating in ratings) {
            val q = state!!["question"]
            val (code, next) = call("POST", "/api/v1/interview/$sessionId/answer", token,
                mapOf(
                    "questionId" to q["questionId"].asLong(),
                    "userText" to "мой ответ на вопрос",
                    "rating" to rating,
                    "secondsSpent" to 95,
                ))
            assert(code == 200) { "$code $next" }
            state = next
        }
        assert(state == null || state["question"] == null || state["question"].isNull) { state.toString() }

        // 3. отчёт
        val (c3, report) = call("POST", "/api/v1/interview/$sessionId/finish", token)
        assert(c3 == 200) { "$c3 $report" }
        assert(report!!["answeredCount"].asInt() == 5)
        assert(report["byRating"]["GOOD"].asInt() == 2)
        assert(report["items"].size() == 5)
        assert(report["items"][0]["referenceAnswer"].asText().isNotBlank())
        assert(report["weakCategories"].size() >= 1)

        // 4. SM-2 получила ответы
        val (c4, queue) = call("GET", "/api/v1/practice/queue?limit=50", token)
        assert(c4 == 200)
        val scheduledIds = queue!!.map { it["questionId"].asLong() }
        val answeredIds = report["items"].map { it["questionId"].asLong() }
        assert(answeredIds.all { it in scheduledIds }) { "интервью не попало в SM-2: $scheduledIds" }

        // 5. повторный ответ -> 409/400 (сессия завершена)
        val (c5) = call("POST", "/api/v1/interview/$sessionId/answer", token,
            mapOf("questionId" to answeredIds[0], "userText" to "x", "rating" to "GOOD"))
        assert(c5 == 409 || c5 == 400) { c5 }

        // 6. чужая сессия -> 404
        val other = login(7733002L)
        val (c6) = call("GET", "/api/v1/interview/$sessionId/state", other)
        assert(c6 == 404) { c6 }
    }

    companion object {
        @JvmStatic
        @Container
        @ServiceConnection
        private val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
    }
}
