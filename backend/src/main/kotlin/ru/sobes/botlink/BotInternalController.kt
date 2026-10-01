package ru.sobes.botlink

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
 * Внутренние эндпоинты для бота. Закрыты общим секретом (X-Bot-Secret),
 * который знают только backend и bot (env BOT_INTERNAL_SECRET).
 */
@RestController
@RequestMapping("/api/v1/bot/internal")
class BotInternalController(
    private val telegramAccounts: TelegramAccountRepository,
    private val linkService: BotLinkService,
    @Value("\${sobes.bot.internal-secret:}") private val internalSecret: String,
) {

    @PostMapping("/lookup")
    fun lookup(
        @RequestHeader("X-Bot-Secret") secret: String?,
        @RequestBody body: LookupRequest,
    ): Map<String, Long> {
        checkSecret(secret)
        val account = telegramAccounts.findByTelegramId(body.telegramId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "не привязан")
        return mapOf("userId" to account.userId)
    }

    @PostMapping("/link-token")
    fun linkToken(
        @RequestHeader("X-Bot-Secret") secret: String?,
        @RequestBody body: LookupRequest,
    ): Map<String, String> {
        checkSecret(secret)
        val webBase = "http://localhost:3000" // TODO(SOBES-35): SOBES_WEB_BASE_URL из env
        val url = linkService.issueLink(body.telegramId, webBase)
        return mapOf("linkUrl" to url)
    }

    private fun checkSecret(secret: String?) {
        if (internalSecret.isBlank()) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "BOT_INTERNAL_SECRET не настроен")
        }
        if (secret != internalSecret) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "неверный секрет бота")
        }
    }
}

data class LookupRequest(val telegramId: Long)
