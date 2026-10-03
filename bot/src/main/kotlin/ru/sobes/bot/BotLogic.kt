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
        val callback = update["callback_query"]
        if (callback != null && !callback.isNull) {
            onCallback(callback)
            return
        }
        val message = update["message"] ?: return
        val chatId = message["chat"]["id"].asLong()
        val from = message["from"]
        val telegramId = from?.get("id")?.asLong() ?: return
        val text = message["text"]?.asText()?.trim() ?: return

        when {
            text == "/start" -> onStart(chatId, telegramId, from)
            text == "/practice" -> onPractice(chatId, telegramId)
            text == "/help" -> telegram.sendMessage(
                chatId,
                "Sobes — подготовка к собеседованиям.\n\n" +
                    "/start — привязать аккаунт сайта\n" +
                    "/practice — потренироваться прямо здесь\n" +
                    "Ежедневно: вопрос дня и напоминания о повторениях."
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

    /** Доставка напоминания о просроченных повторениях. */
    fun sendReviewReminder(chatId: Long, dueCount: Int, oldestDueHours: Long, practiceUrl: String) {
        val oldest = when {
            oldestDueHours >= 24 -> "самое старое ждёт больше суток"
            oldestDueHours >= 1 -> "самое старое ждёт $oldestDueHours ч"
            else -> "самое старое ждёт меньше часа"
        }
        val text = "\u23F0 Просроченные повторения\n\n" +
            "Накопилось карточек: $dueCount ($oldest).\n" +
            "Разгреби очередь — SM-2 любит регулярность:"
        telegram.sendMessage(
            chatId,
            text,
            """{"inline_keyboard":[[{"text":"К практике","url":"$practiceUrl"}]]}"""
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
    /** /practice: выдать карточку с кнопками самооценки. */
    private fun onPractice(chatId: Long, telegramId: Long) {
        val card = try {
            api.nextCard(telegramId)
        } catch (e: Exception) {
            log.warn("next-card: {}", e.message)
            null
        }
        if (card == null) {
            telegram.sendMessage(
                chatId,
                "Сначала привяжи аккаунт: /start\n\nЕсли уже привязан — очередь пуста, все карточки закрыты. Отличная работа!"
            )
            return
        }
        val keyboard =
            """{"inline_keyboard":[[{"text":"\uD83D\uDD01 Не помню","callback_data":"ans:AGAIN:${card.questionId}"},{"text":"\uD83D\uDE41 Трудно","callback_data":"ans:HARD:${card.questionId}"},{"text":"\uD83D\uDE42 Норм","callback_data":"ans:GOOD:${card.questionId}"},{"text":"\uD83D\uDE00 Легко","callback_data":"ans:EASY:${card.questionId}"}]]}"""
        telegram.sendMessage(
            chatId,
            "\uD83E\uDDEF ${card.category} · ${card.difficulty}\n\n${card.body}\n\nКак ты оцениваешь свой ответ?",
            keyboard
        )
    }

    /** Нажатие кнопки самооценки. */
    private fun onCallback(callback: JsonNode) {
        val data = callback["data"]?.asText() ?: return
        val chatId = callback["message"]?.get("chat")?.get("id")?.asLong() ?: return
        val telegramId = callback["from"]["id"].asLong()
        val callbackId = callback["id"].asText()

        if (!data.startsWith("ans:")) {
            telegram.answerCallbackQuery(callbackId)
            return
        }
        val parts = data.split(":")
        if (parts.size != 3) {
            telegram.answerCallbackQuery(callbackId)
            return
        }
        val rating = parts[1]
        val questionId = parts[2].toLongOrNull() ?: run {
            telegram.answerCallbackQuery(callbackId)
            return
        }

        val ok = try {
            api.answer(telegramId, questionId, rating)
        } catch (e: Exception) {
            log.warn("answer: {}", e.message)
            false
        }
        if (!ok) {
            telegram.answerCallbackQuery(callbackId, "Не получилось — попробуй позже")
            return
        }
        telegram.answerCallbackQuery(callbackId, "Записал: $rating \u2192 следующее повторение по расписанию")
        onPractice(chatId, telegramId) // следующая карточка
    }
}
