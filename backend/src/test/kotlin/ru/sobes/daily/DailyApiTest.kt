package ru.sobes.daily

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import ru.sobes.auth.TestTelegramAuth
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@SpringBootTest(
    properties = [
        "sobes.telegram.bot-token=${TestTelegramAuth.BOT_TOKEN}",
        "sobes.jwt.secret=test-jwt-secret-at-least-32-characters-long",
        "sobes.bot.internal-secret=test-bot-secret",
    ],
)
@Testcontainers(disabledWithoutDocker = true)
class DailyApiTest {

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

    private fun post(path: String, body: String, secret: String?, bearer: String? = null): HttpResponse<String> {
        val b = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:${port()}$path"))
            .header("Content-Type", "application/json")
        secret?.let { b.header("X-Bot-Secret", it) }
        bearer?.let { b.header("Authorization", "Bearer $it") }
        return client.send(b.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString())
    }

    @Test
    fun `вопрос дня полный цикл один вопрос в день`() {
        val tg = 7711001L
        val token = login(tg)

        // 1. без секрета -> 401
        assert(post("/api/v1/bot/internal/daily-digest?limit=10", "", null).statusCode() == 401)

        // 2. время напоминания уже наступило (00:00) + напоминания включены
        val put = post("/api/v1/me/settings",
            """{"timezone":"Europe/Moscow","reminderHour":0,"reminderMinute":0,"remindersEnabled":true}""",
            null, token)
        assert(put.statusCode() == 200) { put.body() }

        // 3. дайджест приходит, ровно один вопрос с телом и ссылкой
        val d1 = post("/api/v1/bot/internal/daily-digest?limit=10", "", "test-bot-secret")
        assert(d1.statusCode() == 200) { d1.body() }
        val arr = mapper.readTree(d1.body())
        assert(arr.isArray && arr.size() >= 1) { d1.body() }
        val mine = arr.firstOrNull { it["telegramId"].asLong() == tg } ?: error(d1.body())
        assert(mine["questionId"].asLong() > 0)
        assert(mine["questionBody"].asText().isNotBlank())
        assert(mine["answerUrl"].asText().contains("/questions/"))

        // 4. повторный вызов в тот же день — юзера нет (ровно один вопрос в день)
        val d2 = post("/api/v1/bot/internal/daily-digest?limit=10", "", "test-bot-secret")
        val arr2 = mapper.readTree(d2.body())
        assert(arr2.firstOrNull { it["telegramId"].asLong() == tg } == null) { d2.body() }

        // 5. мусорный секрет -> 401
        assert(post("/api/v1/bot/internal/daily-digest?limit=10", "", "wrong").statusCode() == 401)
    }

    companion object {
        @JvmStatic
        @Container
        @ServiceConnection
        private val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
    }
}
