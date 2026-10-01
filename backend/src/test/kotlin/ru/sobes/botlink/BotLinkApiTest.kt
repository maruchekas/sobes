package ru.sobes.botlink

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
import ru.sobes.botlink.BotLinkTokenRepository
import ru.sobes.botlink.BotLinkService
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Интеграционные тесты привязки аккаунта к боту. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "sobes.telegram.bot-token=${TestTelegramAuth.BOT_TOKEN}",
        "sobes.jwt.secret=test-jwt-secret-at-least-32-characters-long",
    ],
)
@Testcontainers(disabledWithoutDocker = true)
class BotLinkApiTest {

    @Autowired private lateinit var env: Environment
    @Autowired private lateinit var tokens: BotLinkTokenRepository
    @Autowired private lateinit var service: BotLinkService

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

    private fun post(token: String?, path: String, body: String): HttpResponse<String> {
        val b = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:${port()}$path"))
            .header("Content-Type", "application/json")
        if (token != null) b.header("Authorization", "Bearer $token")
        return client.send(b.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString())
    }

    @Test
    fun `полный цикл токен от бота confirm на сайте аккаунты связаны`() {
        val webUid = 700500L // юзер, вошедший на сайте (через Login Widget)
        val botTgId = 700501L // telegram_id, с которым общается бот
        val webToken = login(webUid)

        // бот выпустил ссылку
        val link = service.issueLink(botTgId, "http://localhost:3000")
        assertTrue(link.contains("/link-bot?token="))
        val linkToken = link.substringAfterLast("=")

        // сайт погасил токен от имени залогиненного юзера
        val r = post(webToken, "/api/v1/bot-link/confirm", """{"token":"$linkToken"}""")
        assertEquals(200, r.statusCode(), r.body())
        val result = mapper.readTree(r.body())
        assertEquals(botTgId, result["telegramId"].asLong())

        // токен одноразовый
        val reuse = post(webToken, "/api/v1/bot-link/confirm", """{"token":"$linkToken"}""")
        assertEquals(409, reuse.statusCode())
    }

    @Test
    fun `мусорный токен 404 и без авторизации 401`() {
        assertEquals(404, post(login(700600L), "/api/v1/bot-link/confirm", """{"token":"nope"}""").statusCode())
        assertEquals(401, post(null, "/api/v1/bot-link/confirm", """{"token":"x"}""").statusCode())
    }

    companion object {
        @JvmStatic
        @Container
        @ServiceConnection
        private val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
    }
}
