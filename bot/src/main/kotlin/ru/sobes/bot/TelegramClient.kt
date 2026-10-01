package ru.sobes.bot

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/** Минимальный клиент Telegram Bot API (long polling). */
class TelegramClient(
    private val token: String,
    private val apiBase: String = "https://api.telegram.org",
) {
    private val mapper = jacksonObjectMapper()
    private val http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private fun call(method: String, body: String): JsonNode? {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$apiBase/bot$token/$method"))
            .timeout(Duration.ofSeconds(60))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw IllegalStateException("Telegram $method -> ${response.statusCode()}: ${response.body().take(300)}")
        }
        val json = mapper.readTree(response.body())
        if (!json["ok"].asBoolean()) {
            throw IllegalStateException("Telegram $method not ok: ${json["description"]?.asText()}")
        }
        return json["result"]
    }

    /** Блокирующий long polling. Возвращает список апдейтов. */
    fun getUpdates(offset: Long, timeoutSeconds: Long = 30): List<JsonNode> {
        val result = call(
            "getUpdates",
            """{"offset":$offset,"timeout":$timeoutSeconds,"allowed_updates":["message","callback_query"]}"""
        ) ?: return emptyList()
        return result.map { it }
    }

    fun sendMessage(chatId: Long, text: String, replyMarkup: String? = null): JsonNode? {
        val payload = mapper.createObjectNode()
        payload.put("chat_id", chatId)
        payload.put("text", text)
        payload.put("parse_mode", "HTML")
        if (replyMarkup != null) {
            payload.set<com.fasterxml.jackson.databind.node.ObjectNode>(
                "reply_markup", mapper.readTree(replyMarkup)
            )
        }
        return call("sendMessage", mapper.writeValueAsString(payload))
    }

    fun answerCallbackQuery(callbackQueryId: String, text: String? = null): JsonNode? {
        val payload = mapper.createObjectNode()
        payload.put("callback_query_id", callbackQueryId)
        if (text != null) payload.put("text", text)
        return call("answerCallbackQuery", mapper.writeValueAsString(payload))
    }
}
