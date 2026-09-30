package ru.sobes.auth

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.core.env.Environment
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Интеграционные тесты входа через Telegram Login Widget.
 * Подпись HMAC считается так же, как её считает Telegram (см. TelegramLoginVerifier),
 * на тестовом bot-token, заданном через свойства тестового контекста.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "sobes.telegram.bot-token=123456:TEST-token-for-integration-tests",
        "sobes.jwt.secret=test-jwt-secret-at-least-32-characters-long",
    ],
)
@Testcontainers(disabledWithoutDocker = true)
class AuthApiTest {

    @Autowired
    private lateinit var env: Environment

    private val mapper = jacksonObjectMapper()
    private val client = HttpClient.newHttpClient()

    private fun post(path: String, body: String): HttpResponse<String> {
        val port = requireNotNull(env.getProperty("local.server.port"))
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port$path"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        return client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun getWithToken(path: String, token: String?): HttpResponse<String> {
        val port = requireNotNull(env.getProperty("local.server.port"))
        val builder = HttpRequest.newBuilder().uri(URI.create("http://localhost:$port$path"))
        if (token != null) builder.header("Authorization", "Bearer $token")
        return client.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString())
    }

    /** Подписывает параметры виджета тестовым токеном — как это делает Telegram. */
    private fun signedWidgetParams(
        id: Long = 424242L,
        firstName: String = "Александр",
        lastName: String = "Маручек",
        username: String = "maruchekas",
        authDate: Long = System.currentTimeMillis() / 1000,
    ): String {
        val fields = linkedMapOf(
            "auth_date" to authDate.toString(),
            "first_name" to firstName,
            "id" to id.toString(),
            "last_name" to lastName,
            "username" to username,
        )
        val checkString = fields.entries.sortedWith(compareBy { it.key })
            .joinToString("\n") { "${it.key}=${it.value}" }
        val secret = java.security.MessageDigest.getInstance("SHA-256")
            .digest(TEST_BOT_TOKEN.toByteArray())
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(javax.crypto.spec.SecretKeySpec(secret, "HmacSHA256"))
        val hash = mac.doFinal(checkString.toByteArray())
            .joinToString("") { "%02x".format(it) }
        val body = fields.toMutableMap()
        body["hash"] = hash
        return mapper.writeValueAsString(body)
    }

    @Test
    fun `валидный вход создаёт пользователя и выдаёт JWT`() {
        val response = post("/api/v1/auth/telegram", signedWidgetParams())
        assertEquals(200, response.statusCode(), response.body())
        val body: JsonNode = mapper.readTree(response.body())
        assertTrue(body["token"].asText().length > 40, "JWT должен быть непустым")
        assertEquals("Александр Маручек", body["user"]["displayName"].asText())
        assertEquals("maruchekas", body["user"]["telegramUsername"].asText())

        // /me по выданному токену возвращает того же пользователя.
        val me = getWithToken("/api/v1/auth/me", body["token"].asText())
        assertEquals(200, me.statusCode(), me.body())
        val meBody: JsonNode = mapper.readTree(me.body())
        assertEquals(body["user"]["id"].asLong(), meBody["id"].asLong())
    }

    @Test
    fun `повторный вход не создаёт дубликат пользователя`() {
        val first = mapper.readTree(post("/api/v1/auth/telegram", signedWidgetParams()).body())
        val second = mapper.readTree(post("/api/v1/auth/telegram", signedWidgetParams()).body())
        assertEquals(first["user"]["id"].asLong(), second["user"]["id"].asLong())
    }

    @Test
    fun `подделанная подпись отклоняется с 400`() {
        val body = signedWidgetParams().replace("Александр", "АлександрX")
        val response = post("/api/v1/auth/telegram", body)
        assertEquals(400, response.statusCode(), response.body())
    }

    @Test
    fun `устаревший auth_date отклоняется`() {
        val stale = System.currentTimeMillis() / 1000 - 2 * 3600
        val response = post("/api/v1/auth/telegram", signedWidgetParams(authDate = stale))
        assertEquals(400, response.statusCode(), response.body())
    }

    @Test
    fun `me без токена отдаёт 401`() {
        assertEquals(401, getWithToken("/api/v1/auth/me", null).statusCode())
    }

    @Test
    fun `мусорный токен отдаёт 401`() {
        assertEquals(401, getWithToken("/api/v1/auth/me", "garbage.token.here").statusCode())
    }

    companion object {
        private const val TEST_BOT_TOKEN = "123456:TEST-token-for-integration-tests"

        @JvmStatic
        @Container
        @ServiceConnection
        private val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
    }
}
