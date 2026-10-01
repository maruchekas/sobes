package ru.sobes.settings

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/** DTO настроек кабинета. reminderTime — часы/минуты локального времени пользователя. */
data class SettingsDto(
    @field:NotBlank
    @field:Size(max = 64)
    val timezone: String,

    @field:Min(0)
    @field:Max(23)
    val reminderHour: Int,

    @field:Min(0)
    @field:Max(59)
    val reminderMinute: Int,

    val remindersEnabled: Boolean,
)

/** Полный ответ кабинета: профиль + настройки (настройки создаются лениво с дефолтами). */
data class CabinetDto(
    val userId: Long,
    val displayName: String,
    val telegramUsername: String?,
    val settings: SettingsDto,
)
