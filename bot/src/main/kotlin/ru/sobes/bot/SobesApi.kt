package ru.sobes.bot

import com.fasterxml.jackson.databind.JsonNode
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/** Клиент публичного REST API платформы Sobes. */
class SobesApi(
    private val baseUrl: String,
    private val botSecret: String,
) {

    private val http = HttpClient.newHttpClient()

    /** Есть ли привязанный веб-аккаунт у telegram_id (для /start-логики). */
    fun linkedUserIdOrNull(telegramId: Long): Long? {
        val body = """{"telegramId":$telegramId}"""
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/v1/bot/internal/lookup"))
            .header("Content-Type", "application/json")
            .header("X-Bot-Secret", botSecret)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() == 404) return null
        if (response.statusCode() != 200) {
            throw IllegalStateException("lookup -> ${response.statusCode()}")
        }
        val json = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().readTree(response.body())
        return json["userId"]?.asLong()
    }

    /** Выпустить ссылку привязки (backend создаст токен). */
    fun issueBotLink(telegramId: Long): String {
        val body = """{"telegramId":$telegramId}"""
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/v1/bot/internal/link-token"))
            .header("Content-Type", "application/json")
            .header("X-Bot-Secret", botSecret)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw IllegalStateException("link-token -> ${response.statusCode()}: ${response.body().take(200)}")
        }
        val json = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().readTree(response.body())
        return json["linkUrl"].asText()
    }

    /** Пачка дайджестов «вопрос дня» для рассылки (может быть пустой). */
    fun dailyDigest(limit: Int = 50): List<DailyDigest> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/v1/bot/internal/daily-digest?limit=$limit"))
            .header("X-Bot-Secret", botSecret)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() == 401) return emptyList() // секрет не принят — ретраится позже
        if (response.statusCode() != 200) {
            throw IllegalStateException("daily-digest -> ${response.statusCode()}: ${response.body().take(200)}")
        }
        val json = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().readTree(response.body())
        return json.map { node ->
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
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/v1/bot/internal/review-reminders?limit=$limit"))
            .header("X-Bot-Secret", botSecret)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() == 401) return emptyList()
        if (response.statusCode() != 200) {
            throw IllegalStateException("review-reminders -> ${response.statusCode()}: ${response.body().take(200)}")
        }
        val json = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().readTree(response.body())
        return json.map { node ->
            ReviewReminder(
                telegramId = node["telegramId"].asLong(),
                dueCount = node["dueCount"].asInt(),
                oldestDueHours = node["oldestDueHours"].asLong(),
            )
        }
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
