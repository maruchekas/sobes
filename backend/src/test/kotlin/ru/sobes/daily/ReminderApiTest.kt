package ru.sobes.daily

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.core.env.Environment
import org.springframework.test.context.TestPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import ru.sobes.auth.TestTelegramAuth
import ru.sobes.practice.ReviewSchedule
import ru.sobes.practice.ReviewScheduleId
import ru.sobes.practice.ReviewScheduleRepository
import ru.sobes.settings.UserSettingsRepository
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@SpringBootTest(
    properties = [
        "sobes.telegram.bot-token=${TestTelegramAuth.BOT_TOKEN}",
        "sobes.jwt.secret=test-jwt-secret-at-least-32-characters-long",
        "sobes.bot.internal-secret=test-bot-secret",
    ],
)
@Testcontainers(disabledWithoutDocker = true)
class ReminderApiTest {

    @Autowired private lateinit var env: Environment
    @Autowired private lateinit var schedules: ReviewScheduleRepository
    @Autowired private lateinit var settingsRepo: UserSettingsRepository

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

    private fun post(path: String, secret: String?): HttpResponse<String> {
        val b = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:${port()}$path"))
            .header("Content-Type", "application/json")
        secret?.let { b.header("X-Bot-Secret", it) }
        return client.send(b.POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString())
    }

    private fun addDueCard(userId: Long, questionId: Long, dueAt: Instant) {
        schedules.save(
            ReviewSchedule(
                userId = userId,
                questionId = questionId,
                easeFactor = 2.5,
                intervalDays = 0,
                repetitions = 0,
                dueAt = dueAt,
            )
        )
    }

    @Test
    fun `напоминание приходит раз в сутки при просрочке`() {
        val tg = 7722001L
        val token = login(tg)
        val userId = tgToUserId(token)

        // время напоминания уже наступило
        val settings = settingsRepo.findById(userId).orElse(ru.sobes.settings.UserSettings(userId = userId))
        settings.reminderTime = LocalTime.of(0, 0)
        settings.remindersEnabled = true
        settingsRepo.save(settings)

        // 1. без секрета -> 401
        assert(post("/api/v1/bot/internal/review-reminders?limit=10", null).statusCode() == 401)

        // 2. пока нет просроченных — пусто
        val empty = post("/api/v1/bot/internal/review-reminders?limit=10", "test-bot-secret")
        assert(empty.statusCode() == 200)
        val arrEmpty = mapper.readTree(empty.body())
        assert(arrEmpty.firstOrNull { it["telegramId"].asLong() == tg } == null) { empty.body() }

        // 3. карточка просрочена на 30 часов -> напоминание
        addDueCard(userId, questionId = 1, dueAt = Instant.now().minusSeconds(30 * 3600))
        val r1 = post("/api/v1/bot/internal/review-reminders?limit=10", "test-bot-secret")
        assert(r1.statusCode() == 200) { r1.body() }
        val arr1 = mapper.readTree(r1.body())
        val mine = arr1.firstOrNull { it["telegramId"].asLong() == tg } ?: error(r1.body())
        assert(mine["dueCount"].asInt() >= 1)
        assert(mine["oldestDueHours"].asLong() >= 29)

        // 4. антиспам: повторный вызов в течение 24ч — юзера нет
        val r2 = post("/api/v1/bot/internal/review-reminders?limit=10", "test-bot-secret")
        val arr2 = mapper.readTree(r2.body())
        assert(arr2.firstOrNull { it["telegramId"].asLong() == tg } == null) { r2.body() }
    }

    private fun tgToUserId(token: String): Long {
        val me = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:${port()}/api/v1/auth/me"))
                .header("Authorization", "Bearer $token")
                .GET().build(),
            HttpResponse.BodyHandlers.ofString()
        )
        return mapper.readTree(me.body())["id"].asLong()
    }

    companion object {
        @JvmStatic
        @Container
        @ServiceConnection
        private val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
    }
}
