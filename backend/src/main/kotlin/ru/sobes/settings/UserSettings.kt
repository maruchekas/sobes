package ru.sobes.settings

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalTime

@Entity
@Table(name = "user_settings")
class UserSettings(
    @Id
    @Column(name = "user_id")
    val userId: Long = 0,

    @Column(name = "timezone", nullable = false, length = 64)
    var timezone: String = "Europe/Moscow",

    @Column(name = "reminder_time", nullable = false)
    var reminderTime: LocalTime = LocalTime.of(10, 0),

    @Column(name = "reminders_enabled", nullable = false)
    var remindersEnabled: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
