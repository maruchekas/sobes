package ru.sobes.dashboard

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import ru.sobes.auth.TelegramAccountRepository

/**
 * internal-API для бота: краткая статистика юзера (SOBES-25, /stats).
 * Переиспользует дашборд-логику, отдаёт компактный DTO.
 */
@RestController
@RequestMapping("/api/v1/bot/internal")
class BotStatsController(
    private val dashboardService: DashboardService,
    private val telegramAccounts: TelegramAccountRepository,
    @Value("\${sobes.bot.internal-secret:}") private val internalSecret: String,
) {
    @PostMapping("/stats")
    fun stats(
        @RequestHeader(value = "X-Bot-Secret", required = false) botSecret: String?,
        @RequestBody body: Map<String, Long>,
    ): BotStatsDto {
        if (internalSecret.isBlank()) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "BOT_INTERNAL_SECRET не настроен")
        }
        if (botSecret != internalSecret) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        }
        val telegramId = body["telegramId"]
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "telegramId обязателен")
        val userId = telegramAccounts.findByTelegramId(telegramId)?.userId
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "аккаунт не привязан — /start")

        val d = dashboardService.dashboard(userId)
        return BotStatsDto(
            answeredTotal = d.answeredTotal,
            streakDays = d.streakDays,
            readinessPercent = d.readinessPercent,
            startedQuestions = d.categoryProgress.sumOf { it.answered },
            weakestTopics = d.weakestTopics.take(3).map { WeakestTopic(title = it.title, confidencePercent = it.confidencePercent) },
            dashboardUrl = null, // заполняет бот (знает свой WEB_BASE_URL)
        )
    }
}

data class BotStatsDto(
    val answeredTotal: Long,
    val streakDays: Int,
    val readinessPercent: Int,
    val startedQuestions: Long,
    val weakestTopics: List<WeakestTopic>,
    val dashboardUrl: String?,
)

data class WeakestTopic(
    val title: String,
    val confidencePercent: Int,
)
