package ru.sobes.interview.llm

import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Оценка ответов через DeepSeek (OpenAI-совместимый /chat/completions).
 * Возвращает null при любой проблеме — сервис деградирует на самооценку с эталоном.
 */
@Component
@ConditionalOnProperty("sobes.llm.api-key")
class DeepSeekEvaluator(
    @Value("\${sobes.llm.api-key}") private val apiKey: String,
    @Value("\${sobes.llm.base-url:https://api.deepseek.com}") private val baseUrl: String,
    @Value("\${sobes.llm.model:deepseek-chat}") private val model: String,
) : AnswerEvaluator {

    private val log = LoggerFactory.getLogger(javaClass)
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
    private val mapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()

    companion object {
        private val PROMPT = """
            Ты — строгий, но доброжелательный технический интервьюер. Сравни ответ кандидата с эталонным ответом на вопрос собеседования.

            Правила:
            - Оценивай покрытие сути, а не стиль и длину.
            - score: 0-100 — насколько ответ покрывает ключевые мысли эталона.
            - covered: 1-2 предложения — что кандидат раскрыл верно.
            - missing: 1-2 предложения — какие важные мысли эталона пропущены или неверны.
            - verdict: одна короткая фраза-резюме.
            - Отвечай ТОЛЬКО валидным JSON без markdown-обёрток:
              {"score": <int>, "covered": "...", "missing": "...", "verdict": "..."}
            - Всё по-русски.
        """.trimIndent()
    }

    override fun evaluate(questionBody: String, referenceAnswer: String, userAnswer: String): Evaluation? {
        if (userAnswer.isBlank()) return null
        val userMsg = """
            Вопрос: $questionBody

            Эталонный ответ: ${referenceAnswer.take(4000)}

            Ответ кандидата: ${userAnswer.take(4000)}
        """.trimIndent()

        val payload = mapper.createObjectNode().apply {
            put("model", model)
            put("temperature", 0.2)
            put("max_tokens", 400)
            putArray("messages").apply {
                addObject().apply {
                    put("role", "system")
                    put("content", PROMPT)
                }
                addObject().apply {
                    put("role", "user")
                    put("content", userMsg)
                }
            }
        }

        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/chat/completions"))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $apiKey")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                .build()
            val response = http.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() != 200) {
                log.warn("LLM вернул {}: {}", response.statusCode(), response.body().take(200))
                return null
            }
            val content = mapper.readTree(response.body())["choices"][0]["message"]["content"].asText()
            parse(content)
        } catch (e: Exception) {
            log.warn("LLM-оценка недоступна: {}", e.message)
            null
        }
    }

    /** Модель любит обернуть JSON в ```json ... ``` — вырезаем. */
    private fun parse(content: String): Evaluation? = try {
        val cleaned = content.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val node: JsonNode = mapper.readTree(cleaned)
        Evaluation(
            score = node["score"].asInt().coerceIn(0, 100),
            covered = node["covered"].asText(),
            missing = node["missing"].asText(),
            verdict = node["verdict"].asText(),
        )
    } catch (e: Exception) {
        log.warn("LLM-ответ не разобран: {}", content.take(150))
        null
    }
}
