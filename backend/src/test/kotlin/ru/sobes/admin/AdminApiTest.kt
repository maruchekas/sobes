package ru.sobes.admin

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
        "sobes.admin.telegram-ids=7744001",
    ],
)
@Testcontainers(disabledWithoutDocker = true)
class AdminApiTest {

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
            "DELETE" -> b.DELETE()
            else -> b.POST(HttpRequest.BodyPublishers.ofString(body?.let { mapper.writeValueAsString(it) } ?: ""))
        }
        val resp = client.send(b.build(), HttpResponse.BodyHandlers.ofString())
        val tree = runCatching { mapper.readTree(resp.body()) }.getOrNull()
        return resp.statusCode() to tree
    }

    @Test
    fun `админ управляет банком, обычный юзер получает 403`() {
        val admin = login(7744001L)   // в sobes.admin.telegram-ids
        val mortal = login(7744002L)  // обычный

        // 1. не-админ -> 403
        val (c1) = call("GET", "/api/v1/admin/questions", mortal)
        assert(c1 == 403) { c1 }

        // 2. админ видит список
        val (c2, list) = call("GET", "/api/v1/admin/questions?size=5", admin)
        assert(c2 == 200 && (list!!["totalElements"].asLong()) > 100) { "$c2 $list" }

        // 3. создаёт вопрос
        val (c3, created) = call("POST", "/api/v1/admin/questions", admin,
            mapOf(
                "categorySlug" to "java-core",
                "difficulty" to "MIDDLE",
                "body" to "Тестовый вопрос админки: что такое разделитель?",
                "answer" to "Тестовый эталонный ответ.",
                "followup" to "А углубление?",
            ))
        assert(c3 == 200 && created!!["id"].asLong() > 0) { "$c3 $created" }
        val newId = created!!["id"].asLong()

        // 4. редактирует
        val (c4, updated) = call("POST", "/api/v1/admin/questions", admin,
            mapOf(
                "id" to newId,
                "categorySlug" to "kotlin",
                "difficulty" to "SENIOR",
                "body" to "Тестовый вопрос (правка): что такое разделитель?",
                "answer" to "Обновлённый эталон.",
            ))
        assert(c4 == 200 && updated!!["categorySlug"].asText() == "kotlin") { "$c4 $updated" }

        // 5. поиск находит по фрагменту
        val (c5, found) = call("GET", "/api/v1/admin/questions?search=Тестовый вопрос (правка)", admin)
        assert(c5 == 200 && found!!["content"].size() >= 1) { "$c5 $found" }

        // 6. категории
        val (c6, cats) = call("GET", "/api/v1/admin/categories", admin)
        assert(c6 == 200 && cats != null && cats.size() > 10)
        val (c7, newCat) = call("POST", "/api/v1/admin/categories", admin,
            mapOf("slug" to "test-cat", "title" to "Тестовая категория", "sortOrder" to 999))
        assert(c7 == 200 && newCat!!["slug"].asText() == "test-cat")

        // 7. мусорная категория в вопросе -> 400
        val (c8) = call("POST", "/api/v1/admin/questions", admin,
            mapOf("categorySlug" to "no-such", "difficulty" to "MIDDLE", "body" to "x", "answer" to "y"))
        assert(c8 == 400) { c8 }

        // 8. удаление
        val (c9) = call("DELETE", "/api/v1/admin/questions/$newId", admin)
        assert(c9 == 200) { c9 }
        val (c10) = call("DELETE", "/api/v1/admin/questions/$newId", admin)
        assert(c10 == 404) { c10 }
    }

    companion object {
        @JvmStatic
        @Container
        @ServiceConnection
        private val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
    }
}
