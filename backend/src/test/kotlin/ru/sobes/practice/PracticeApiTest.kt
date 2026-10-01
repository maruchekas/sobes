package ru.sobes.practice

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
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Интеграционные тесты практики: очередь, самооценка, SM-2 в БД. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "sobes.telegram.bot-token=${TestTelegramAuth.BOT_TOKEN}",
        "sobes.jwt.secret=test-jwt-secret-at-least-32-characters-long",
    ],
)
@Testcontainers(disabledWithoutDocker = true)
class PracticeApiTest {

    @Autowired
    private lateinit var env: Environment

    private val mapper = jacksonObjectMapper()
    private val client = HttpClient.newHttpClient()

    private fun port() = requireNotNull(env.getProperty("local.server.port"))

    private fun call(method: String, path: String, token: String?, body: String? = null): HttpResponse<String> {
        val builder = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:${port()}$path"))
            .header("Content-Type", "application/json")
        if (token != null) builder.header("Authorization", "Bearer $token")
        if (body != null) builder.method(method, HttpRequest.BodyPublishers.ofString(body))
        else builder.method(method, HttpRequest.BodyPublishers.noBody())
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    }

    private fun login(): String {
        val r = call("POST", "/api/v1/auth/telegram", null,
            mapper.writeValueAsString(TestTelegramAuth.signedWidgetParams(id = 800100L)))
        return mapper.readTree(r.body())["token"].asText()
    }

    @Test
    fun `очередь содержит только новые карточки для нового пользователя`() {
        val token = login()
        val r = call("GET", "/api/v1/practice/queue?limit=3", token)
        assertEquals(200, r.statusCode(), r.body())
        val queue = mapper.readTree(r.body())
        assertEquals(3, queue.size())
        assertTrue(queue.all { it["state"].asText() == "NEW" }, "все карточки должны быть NEW")
        assertTrue(queue[0]["body"].asText().isNotBlank())
        assertTrue(queue[0]["category"].asText().isNotBlank())
    }

    @Test
    fun `ответ GOOD планирует повторение через день и пишет историю`() {
        val token = login()
        val queue = mapper.readTree(call("GET", "/api/v1/practice/queue?limit=1", token).body())
        val qid = queue[0]["questionId"].asLong()

        val answer = call("POST", "/api/v1/practice/answer", token,
            """{"questionId":$qid,"rating":"GOOD"}""")
        assertEquals(200, answer.statusCode(), answer.body())
        val result = mapper.readTree(answer.body())
        assertEquals(1, result["intervalDays"].asInt())

        // карточка больше не NEW и не due до истечения интервала
        val summary = mapper.readTree(call("GET", "/api/v1/practice/summary", token).body())
        assertEquals(0, summary["dueCount"].asInt())
        assertEquals(1, summary["answeredTotal"].asInt())
    }

    @Test
    fun `ответ AGAIN держит карточку в due на завтра`() {
        val token = login()
        val queue = mapper.readTree(call("GET", "/api/v1/practice/queue?limit=1", token).body())
        val qid = queue[0]["questionId"].asLong()

        val answer = call("POST", "/api/v1/practice/answer", token,
            """{"questionId":$qid,"rating":"AGAIN"}""")
        assertEquals(200, answer.statusCode(), answer.body())

        val summary = mapper.readTree(call("GET", "/api/v1/practice/summary", token).body())
        // due завтра — сегодня не due
        assertEquals(0, summary["dueCount"].asInt())
    }

    @Test
    fun `некорректная оценка отклоняется с 400`() {
        val token = login()
        val r = call("POST", "/api/v1/practice/answer", token,
            """{"questionId":1,"rating":"SUPER"}""")
        assertEquals(400, r.statusCode(), r.body())
    }

    @Test
    fun `практика без токена отдаёт 401`() {
        assertEquals(401, call("GET", "/api/v1/practice/queue", null).statusCode())
    }

    companion object {
        @JvmStatic
        @Container
        @ServiceConnection
        private val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
    }
}
