package ru.sobes.daily

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * internal-API для бота: пачка дайджестов «вопрос дня».
 * Закрыт общим секретом sobes.bot.internal-secret (env BOT_INTERNAL_SECRET).
 */
@RestController
@RequestMapping("/api/v1/bot/internal")
class DailyInternalController(
    private val dailyDigestService: DailyDigestService,
    @Value("\${sobes.bot.internal-secret:}") private val internalSecret: String,
) {
    @PostMapping("/daily-digest")
    fun nextBatch(
        @RequestHeader(value = "X-Bot-Secret", required = false) botSecret: String?,
        @RequestParam(defaultValue = "50") limit: Int,
    ): List<DailyDigestDto> {
        if (internalSecret.isBlank()) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "BOT_INTERNAL_SECRET не настроен")
        }
        if (botSecret != internalSecret) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        }
        return dailyDigestService.nextBatch(minOf(limit, 200))
    }
}
