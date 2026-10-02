package ru.sobes.bot

import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Скелет бота: /start (приветствие или ссылка привязки), /help.
 * Long polling, один поток, состояние — только offset апдейтов.
 */
class BotLogic(
    private val telegram: TelegramClient,
    private val api: SobesApi,
) {
    private val log = LoggerFactory.getLogger("bot")
    private val offset = AtomicLong(0)
    private val running = AtomicReference(false)

    fun start() {
        if (!running.compareAndSet(false, true)) return
        log.info("бот запущен, long polling...")
        while (running.get()) {
            try {
                val updates = telegram.getUpdates(offset.get())
                for (update in updates) {
                    handle(update)
                    offset.set(update["update_id"].asLong() + 1)
                }
            } catch (e: Exception) {
                log.error("ошибка цикла polling: ${e.message}")
                Thread.sleep(3000)
            }
        }
    }

    fun stop() = running.set(false)

    private fun handle(update: JsonNode) {
        val message = update["message"] ?: return
        val chatId = message["chat"]["id"].asLong()
        val from = message["from"]
        val telegramId = from?.get("id")?.asLong() ?: return
        val text = message["text"]?.asText()?.trim() ?: return

        when {
            text == "/start" -> onStart(chatId, telegramId, from)
            text == "/help" -> telegram.sendMessage(
                chatId,
                "Sobes — подготовка к собеседованиям.\n\n" +
                    "/start — привязать аккаунт сайта\n" +
                    "Скоро: вопрос дня и напоминания о повторениях."
            )
            else -> telegram.sendMessage(chatId, "Пока понимаю только /start и /help 🙂")
        }
    }

    private fun onStart(chatId: Long, telegramId: Long, from: JsonNode) {
        val firstName = from["first_name"]?.asText() ?: "привет"
        val linked = try {
            api.linkedUserIdOrNull(telegramId)
        } catch (e: Exception) {
            log.warn("lookup недоступен: ${e.message}")
            telegram.sendMessage(
                chatId,
                "Не удалось связаться с платформой. Попробуйте позже."
            )
            return
        }

        if (linked != null) {
            telegram.sendMessage(
                chatId,
                "С возвращением, $firstName! Аккаунт связан — напоминания о повторениях скоро заработают."
            )
            return
        }

        val linkUrl = try {
            api.issueBotLink(telegramId)
        } catch (e: Exception) {
            log.error("link-token недоступен: ${e.message}")
            null
        }

        if (linkUrl == null) {
            telegram.sendMessage(
                chatId,
                "Привет, $firstName! Чтобы связать бота с аккаунтом, войдите на сайте — привязка скоро заработает."
            )
            return
        }

        telegram.sendMessage(
            chatId,
            "Привет, $firstName! Я — бот платформы Sobes.\n\n" +
                "Чтобы я знал, какие вопросы тебе присылать, привяжи аккаунт сайта:",
            """{"inline_keyboard":[[{"text":"Привязать аккаунт","url":"$linkUrl"}]]}"""
        )
    }

    /** Доставка «вопроса дня» (вызывается из digest-потока Main.kt). */
    fun sendDailyQuestion(chatId: Long, questionBody: String, answerUrl: String) {
        val text = "\uD83C\uDFAF Вопрос дня\n\n" + questionBody + "\n\nПодумай над ответом, затем сверься с эталоном:"
        telegram.sendMessage(
            chatId,
            text,
            """{"inline_keyboard":[[{"text":"Открыть ответ","url":"$answerUrl"}]]}"""
        )
    }
}
