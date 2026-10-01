package ru.sobes.bot

import com.fasterxml.jackson.databind.JsonNode
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/** Клиент публичного REST API платформы Sobes. */
class SobesApi(private val baseUrl: String) {

    private val http = HttpClient.newHttpClient()

    /** Есть ли привязанный веб-аккаунт у telegram_id (для /start-логики). */
    fun linkedUserIdOrNull(telegramId: Long): Long? {
        val body = """{"telegramId":$telegramId}"""
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/v1/bot/internal/lookup"))
            .header("Content-Type", "application/json")
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
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw IllegalStateException("link-token -> ${response.statusCode()}: ${response.body().take(200)}")
        }
        val json = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().readTree(response.body())
        return json["linkUrl"].asText()
    }
}
