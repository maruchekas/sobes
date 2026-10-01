package ru.sobes.settings

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

/**
 * Интеграционные тесты личного кабинета: дефолты настроек, обновление, валидация таймзоны.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "sobes.telegram.bot-token=${TestTelegramAuth.BOT_TOKEN}",
        "sobes.jwt.secret=test-jwt-secret-at-least-32-characters-long",
    ],
)
@Testcontainers(disabledWithoutDocker = true)
class SettingsApiTest {

    @Autowired
    private lateinit var env: Environment

    private val mapper = jacksonObjectMapper()
    private val client = HttpClient.newHttpClient()

    private fun port() = requireNotNull(env.getProperty("local.server.port"))

    private fun request(method: String, path: String, token: String?, body: String? = null): HttpResponse<String> {
        val builder = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:${port()}$path"))
            .header("Content-Type", "application/json")
        if (token != null) builder.header("Authorization", "Bearer $token")
        if (body != null) {
            builder.method(method, HttpRequest.BodyPublishers.ofString(body))
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody())
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    }

    /** Логин + токен одним вызовом. */
    private fun login(): String {
        val login = request("POST", "/api/v1/auth/telegram", null,
            mapper.writeValueAsString(TestTelegramAuth.signedWidgetParams()))
        assertEquals(200, login.statusCode(), login.body())
        return mapper.readTree(login.body())["token"].asText()
    }

    @Test
    fun `кабинет отдаёт профиль и дефолтные настройки`() {
        val token = login()
        val response = request("GET", "/api/v1/me/settings", token)
        assertEquals(200, response.statusCode(), response.body())
        val body = mapper.readTree(response.body())
        assertEquals("Europe/Moscow", body["settings"]["timezone"].asText())
        assertEquals(10, body["settings"]["reminderHour"].asInt())
        assertEquals(0, body["settings"]["reminderMinute"].asInt())
        assertEquals(true, body["settings"]["remindersEnabled"].asBoolean())
        assertEquals("Тест Тестов", body["displayName"].asText())
    }

    @Test
    fun `обновление настроек сохраняется и читается обратно`() {
        val token = login()
        val update = """{"timezone":"Europe/Berlin","reminderHour":19,"reminderMinute":30,"remindersEnabled":false}"""
        val put = request("PUT", "/api/v1/me/settings", token, update)
        assertEquals(200, put.statusCode(), put.body())

        val get = request("GET", "/api/v1/me/settings", token)
        val body = mapper.readTree(get.body())
        assertEquals("Europe/Berlin", body["settings"]["timezone"].asText())
        assertEquals(19, body["settings"]["reminderHour"].asInt())
        assertEquals(30, body["settings"]["reminderMinute"].asInt())
        assertEquals(false, body["settings"]["remindersEnabled"].asBoolean())
    }

    @Test
    fun `несуществующая таймзона отклоняется с 400`() {
        val token = login()
        val update = """{"timezone":"Mars/Olympus","reminderHour":10,"reminderMinute":0,"remindersEnabled":true}"""
        assertEquals(400, request("PUT", "/api/v1/me/settings", token, update).statusCode())
    }

    @Test
    fun `час вне диапазона отклоняется с 400`() {
        val token = login()
        val update = """{"timezone":"Europe/Moscow","reminderHour":25,"reminderMinute":0,"remindersEnabled":true}"""
        assertEquals(400, request("PUT", "/api/v1/me/settings", token, update).statusCode())
    }

    @Test
    fun `кабинет без токена отдаёт 401`() {
        assertEquals(401, request("GET", "/api/v1/me/settings", null).statusCode())
    }

    companion object {
        @JvmStatic
        @Container
        @ServiceConnection
        private val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
    }
}
