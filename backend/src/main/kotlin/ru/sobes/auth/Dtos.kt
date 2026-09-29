package ru.sobes.auth

import java.time.Instant

/** Данные авторизации из Telegram Login Widget (проверенные по HMAC-SHA256). */
data class TelegramAuth(
    val id: Long,
    val first_name: String?,
    val last_name: String?,
    val username: String?,
    val photo_url: String?,
    val auth_date: Long,
)

/** Пользователь платформы. */
data class UserView(
    val id: Long,
    val displayName: String,
    val telegramUsername: String?,
    val createdAt: Instant,
)

data class LoginResponse(val token: String, val user: UserView)
