package ru.sobes.bot

import com.fasterxml.jackson.databind.JsonNode
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/** Клиент REST API платформы Sobes (internal-эндпоинты под X-Bot-Secret). */
class SobesApi(
    private val baseUrl: String,
    private val botSecret: String,
) {

    private val http = HttpClient.newHttpClient()
    private val mapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()

    /** Есть ли привязанный веб-аккаунт у telegram_id (для /start-логики). */
    fun linkedUserIdOrNull(telegramId: Long): Long? {
        val resp = post("/api/v1/bot/internal/lookup", """{"telegramId":$telegramId}""")
        if (resp.statusCode() == 404) return null
        return checkOk("lookup", resp)["userId"]?.asLong()
    }

    /** Выпустить ссылку привязки (backend создаст токен). */
    fun issueBotLink(telegramId: Long): String {
        val resp = post("/api/v1/bot/internal/link-token", """{"telegramId":$telegramId}""")
        return checkOk("link-token", resp)["linkUrl"].asText()
    }

    /** Пачка дайджестов «вопрос дня» для рассылки (может быть пустой). */
    fun dailyDigest(limit: Int = 50): List<DailyDigest> {
        val resp = post("/api/v1/bot/internal/daily-digest?limit=$limit", "")
        if (resp.statusCode() == 401) return emptyList()
        return checkOk("daily-digest", resp).map { node ->
            DailyDigest(
                telegramId = node["telegramId"].asLong(),
                questionId = node["questionId"].asLong(),
                questionBody = node["questionBody"].asText(),
                answerUrl = node["answerUrl"].asText(),
            )
        }
    }

    /** Пачка напоминаний о просроченных повторениях. */
    fun reviewReminders(limit: Int = 50): List<ReviewReminder> {
        val resp = post("/api/v1/bot/internal/review-reminders?limit=$limit", "")
        if (resp.statusCode() == 401) return emptyList()
        return checkOk("review-reminders", resp).map { node ->
            ReviewReminder(
                telegramId = node["telegramId"].asLong(),
                dueCount = node["dueCount"].asInt(),
                oldestDueHours = node["oldestDueHours"].asLong(),
            )
        }
    }

    /** Карточка для практики в боте (null — очередь пуста или аккаунт не привязан). */
    fun nextCard(telegramId: Long): BotCard? {
        val resp = post("/api/v1/bot/internal/next-card", """{"telegramId":$telegramId}""")
        if (resp.statusCode() == 404) return null
        val json = checkOk("next-card", resp)
        return BotCard(
            questionId = json["questionId"].asLong(),
            body = json["body"].asText(),
            category = json["category"].asText(),
            difficulty = json["difficulty"].asText(),
        )
    }

    /** Отправить самооценку из бота (false — ошибка бэкенда). */
    fun answer(telegramId: Long, questionId: Long, rating: String): Boolean {
        val body = """{"telegramId":$telegramId,"questionId":$questionId,"rating":"$rating"}"""
        return post("/api/v1/bot/internal/answer", body).statusCode() == 200
    }

    /** Статистика юзера для /stats (null — не привязан). */
    fun stats(telegramId: Long): BotStats? {
        val resp = post("/api/v1/bot/internal/stats", """{"telegramId":$telegramId}""")
        if (resp.statusCode() == 404) return null
        val json = checkOk("stats", resp)
        return BotStats(
            answeredTotal = json["answeredTotal"].asLong(),
            streakDays = json["streakDays"].asInt(),
            readinessPercent = json["readinessPercent"].asInt(),
            startedQuestions = json["startedQuestions"].asLong(),
            weakestTopics = json["weakestTopics"].map { t ->
                WeakestTopic(t["title"].asText(), t["confidencePercent"].asInt())
            },
        )
    }

    // ── инфраструктура ──────────────────────────────────────────────

    private fun post(path: String, body: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl$path"))
            .header("Content-Type", "application/json")
            .header("X-Bot-Secret", botSecret)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        return http.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun checkOk(name: String, resp: HttpResponse<String>): JsonNode {
        if (resp.statusCode() != 200) {
            throw IllegalStateException("$name -> ${resp.statusCode()}: ${resp.body().take(200)}")
        }
        return mapper.readTree(resp.body())
    }
}

data class DailyDigest(
    val telegramId: Long,
    val questionId: Long,
    val questionBody: String,
    val answerUrl: String,
)

data class ReviewReminder(
    val telegramId: Long,
    val dueCount: Int,
    val oldestDueHours: Long,
)

data class BotCard(
    val questionId: Long,
    val body: String,
    val category: String,
    val difficulty: String,
)

data class BotStats(
    val answeredTotal: Long,
    val streakDays: Int,
    val readinessPercent: Int,
    val startedQuestions: Long,
    val weakestTopics: List<WeakestTopic>,
)

data class WeakestTopic(
    val title: String,
    val confidencePercent: Int,
)
