package ru.sobes.practice

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import ru.sobes.auth.TelegramAccountRepository
import ru.sobes.question.QuestionService

data class BotCardDto(
    val questionId: Long,
    val body: String,
    val category: String,
    val difficulty: String,
    val state: String,
)

data class BotAnswerRequest(
    val telegramId: Long,
    val questionId: Long,
    val rating: Rating,
)

/**
 * internal-API для бота: карточка для практики прямо в Telegram (SOBES-24).
 * Идентификация по telegram_id + X-Bot-Secret (без JWT — бот не пользователь).
 */
@RestController
@RequestMapping("/api/v1/bot/internal")
class BotPracticeController(
    private val practice: PracticeService,
    private val questionService: QuestionService,
    private val telegramAccounts: TelegramAccountRepository,
    @Value("\${sobes.bot.internal-secret:}") private val internalSecret: String,
) {
    @PostMapping("/next-card")
    fun nextCard(
        @RequestHeader(value = "X-Bot-Secret", required = false) botSecret: String?,
        @RequestBody body: Map<String, Long>,
    ): BotCardDto {
        checkSecret(botSecret)
        val userId = userIdByTelegram(body["telegramId"]
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "telegramId обязателен"))
        val item = practice.nextQueue(userId, 1).firstOrNull()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "очередь пуста — все карточки закрыты")
        val details = questionService.byId(item.questionId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "вопрос исчез из каталога")
        return BotCardDto(
            questionId = item.questionId,
            body = details.body,
            category = details.category,
            difficulty = details.difficulty.name,
            state = item.state.name,
        )
    }

    @PostMapping("/answer")
    fun answer(
        @RequestHeader(value = "X-Bot-Secret", required = false) botSecret: String?,
        @RequestBody body: BotAnswerRequest,
    ): Map<String, Any> {
        checkSecret(botSecret)
        val userId = userIdByTelegram(body.telegramId)
        val result = practice.answer(userId, body.questionId, body.rating)
        return mapOf(
            "nextDueAt" to result.dueAt.toString(),
            "intervalDays" to result.intervalDays,
        )
    }

    private fun userIdByTelegram(telegramId: Long): Long =
        telegramAccounts.findByTelegramId(telegramId)?.userId
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "аккаунт не привязан — /start")

    private fun checkSecret(botSecret: String?) {
        if (internalSecret.isBlank()) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "BOT_INTERNAL_SECRET не настроен")
        }
        if (botSecret != internalSecret) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        }
    }
}
