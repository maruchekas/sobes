package ru.sobes.auth

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/** Регистрация/вход по Telegram-данным, выдача JWT. */
@Service
class AuthService(
    private val verifier: TelegramLoginVerifier,
    private val jwtService: JwtService,
    private val users: UserRepository,
    private val telegramAccounts: TelegramAccountRepository,
) {
    @Transactional
    fun loginByTelegram(params: Map<String, String>): LoginResponse {
        val auth = verifier.verify(params)

        val existing = telegramAccounts.findByTelegramId(auth.id)
        val user: User
        val account: TelegramAccount
        if (existing != null) {
            user = users.getReferenceById(existing.userId)
            account = existing
            // Обновляем свежие данные из Telegram.
            account.username = auth.username
            user.lastSeenAt = Instant.now()
            val freshName = displayName(auth)
            if (freshName.isNotBlank()) user.displayName = freshName
        } else {
            user = users.save(
                User(displayName = displayName(auth).ifBlank { "Пользователь ${auth.id}" })
            )
            account = telegramAccounts.save(
                TelegramAccount(
                    userId = user.id,
                    telegramId = auth.id,
                    username = auth.username,
                    languageCode = params["language_code"],
                )
            )
        }
        return LoginResponse(token = jwtService.issue(user.id), user = user.toView(account))
    }

    @Transactional(readOnly = true)
    fun currentUser(userId: Long): UserView {
        val user = users.findById(userId).orElseThrow {
            IllegalArgumentException("Пользователь не найден")
        }
        val account = telegramAccounts.findByUserId(userId)
        return user.toView(account)
    }

    private fun displayName(auth: TelegramAuth): String =
        listOfNotNull(auth.first_name, auth.last_name).joinToString(" ").trim()

    private fun User.toView(account: TelegramAccount?): UserView = UserView(
        id = id,
        displayName = displayName,
        telegramUsername = account?.username,
        createdAt = createdAt,
    )
}
