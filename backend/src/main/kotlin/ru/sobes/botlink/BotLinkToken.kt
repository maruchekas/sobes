package ru.sobes.botlink

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "bot_link_tokens")
class BotLinkToken(
    @Id
    @Column(name = "token", length = 64)
    val token: String = "",

    @Column(name = "telegram_id", nullable = false)
    val telegramId: Long,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Column(name = "used_at")
    var usedAt: Instant? = null,
)
