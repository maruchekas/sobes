package ru.sobes.question

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
import kotlin.test.assertTrue

/**
 * Интеграционные тесты API на реальном PostgreSQL (Testcontainers).
 * HTTP-клиент — стандартный из JDK, чтобы тесты не зависели от тестовых утилит Spring.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class QuestionApiTest {

    @Autowired
    private lateinit var env: Environment

    private val mapper = jacksonObjectMapper()
    private val client = HttpClient.newHttpClient()

    private fun get(path: String): HttpResponse<String> {
        val port = requireNotNull(env.getProperty("local.server.port"))
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port$path"))
            .GET()
            .build()
        return client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun json(path: String): JsonNode {
        val response = get(path)
        assertEquals(200, response.statusCode(), "GET $path вернул ${response.statusCode()}")
        return mapper.readTree(response.body())
    }

    @Test
    fun `каталог отдаёт весь стартовый банк вопросов`() {
        assertEquals(112, json("/api/v1/questions?size=1")["totalElements"].asInt())
    }

    @Test
    fun `все двадцать две темы доступны в каталоге`() {
        val categories = json("/api/v1/categories")

        assertEquals(22, categories.size())
        assertTrue(categories.any { it["slug"].asText() == "kotlin" && it["questions"].asInt() == 7 })
    }

    @Test
    fun `фильтр по уровню возвращает только senior вопросы`() {
        val items = json("/api/v1/questions?difficulty=SENIOR&size=100")["content"]

        assertTrue(items.size() > 0, "senior-вопросы должны быть в банке")
        assertTrue(items.all { it["difficulty"].asText() == "SENIOR" })
    }

    @Test
    fun `фильтр по теме kotlin возвращает семь вопросов`() {
        assertEquals(7, json("/api/v1/questions?category=kotlin&size=50")["totalElements"].asInt())
    }

    @Test
    fun `вопрос отдаётся вместе с эталонным ответом`() {
        val question = json("/api/v1/questions/1")

        assertTrue(question["body"].asText().isNotBlank())
        assertTrue(question["answer"].asText().isNotBlank())
        assertTrue(question["category"].asText().isNotBlank())
    }

    @Test
    fun `несуществующий вопрос возвращает 404`() {
        assertEquals(404, get("/api/v1/questions/999999").statusCode())
    }

    @Test
    fun `несуществующая тема возвращает пустой список`() {
        assertEquals(0, json("/api/v1/questions?category=no-such-category")["totalElements"].asInt())
    }

    companion object {

        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine")
            .apply {
                withDatabaseName("sobes")
                withUsername("sobes")
                withPassword("sobes")
            }
    }
}
