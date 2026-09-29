package ru.sobes.auth

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "display_name", nullable = false, length = 120)
    var displayName: String = "",

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "last_seen_at")
    var lastSeenAt: Instant? = null,
)

@Entity
@Table(name = "telegram_accounts")
class TelegramAccount(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "telegram_id", nullable = false, unique = true)
    val telegramId: Long,

    @Column(name = "username", length = 64)
    var username: String? = null,

    @Column(name = "language_code", length = 8)
    var languageCode: String? = null,

    @Column(name = "linked_at", nullable = false)
    val linkedAt: Instant = Instant.now(),
)
