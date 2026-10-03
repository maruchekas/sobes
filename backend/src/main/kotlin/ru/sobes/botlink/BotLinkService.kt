package ru.sobes.botlink

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import ru.sobes.auth.TelegramAccountRepository
import ru.sobes.auth.User
import ru.sobes.auth.UserRepository
import java.time.Instant
import java.util.UUID

/**
 * Привязка аккаунта платформы к Telegram-боту.
 *
 * Флоу: бот на /start (незнакомый telegram_id) генерирует токен и показывает ссылку
 * "Привязать аккаунт" (t.me-диплинка не нужна: юзер уже в боте — нужен веб-линк).
 * Токен живёт 24 часа. Юзер открывает сайт (уже залогинен через Login Widget),
 * сайт гасит токен POST /api/v1/bot-link/confirm -> аккаунты связаны.
 */
@Service
class BotLinkService(
    private val tokens: BotLinkTokenRepository,
    private val users: UserRepository,
    private val telegramAccounts: TelegramAccountRepository,
) {

    /** Вызывает бот при /start от незнакомого telegram_id. Возвращает веб-ссылку привязки. */
    @Transactional
    fun issueLink(telegramId: Long, webBaseUrl: String): String {
        val token = UUID.randomUUID().toString().replace("-", "")
        tokens.save(
            BotLinkToken(
                token = token,
                telegramId = telegramId,
                expiresAt = Instant.now().plusSeconds(LINK_TTL_SECONDS),
            )
        )
        return "$webBaseUrl/link-bot?token=$token"
    }

    /** Вызывает сайт от имени залогиненного пользователя. Связывает аккаунт и гасит токен. */
    @Transactional
    fun confirm(userId: Long, token: String): BotLinkResult {
        val entity = tokens.findById(token).orElse(null)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Токен привязки не найден")
        if (entity.usedAt != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Токен уже использован")
        }
        if (entity.expiresAt.isBefore(Instant.now())) {
            throw ResponseStatusException(HttpStatus.GONE, "Токен истёк — запросите новую ссылку в боте")
        }

        val user: User = users.findById(userId).orElse(null)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден")

        // У пользователя уже может быть telegram-аккаунт (вход через виджет) — сверяем telegram_id.
        val existing = telegramAccounts.findByUserId(userId)
        if (existing != null) {
            if (existing.telegramId != entity.telegramId) {
                throw ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "К этому аккаунту уже привязан другой Telegram (${existing.telegramId})"
                )
            }
        } else {
            telegramAccounts.save(
                ru.sobes.auth.TelegramAccount(
                    userId = userId,
                    telegramId = entity.telegramId,
                    username = null,
                )
            )
        }

        entity.usedAt = Instant.now()
        tokens.save(entity)
        return BotLinkResult(linked = true, telegramId = entity.telegramId, displayName = user.displayName)
    }

    companion object {
        private const val LINK_TTL_SECONDS = 24 * 3600L
    }
}

data class BotLinkResult(
    val linked: Boolean,
    val telegramId: Long,
    val displayName: String,
)
