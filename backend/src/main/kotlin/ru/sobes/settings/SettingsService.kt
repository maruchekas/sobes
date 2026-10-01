package ru.sobes.settings

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import ru.sobes.auth.TelegramAccountRepository
import ru.sobes.auth.UserRepository
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/** Чтение/обновление настроек личного кабинета. */
@Service
class SettingsService(
    private val settingsRepository: UserSettingsRepository,
    private val users: UserRepository,
    private val telegramAccounts: TelegramAccountRepository,
) {

    @Transactional
    fun getCabinet(userId: Long): CabinetDto {
        val user = users.findById(userId).orElseThrow { notFound() }
        return CabinetDto(
            userId = user.id,
            displayName = user.displayName,
            telegramUsername = telegramAccounts.findByUserId(userId)?.username,
            settings = ensure(userId).toDto(),
        )
    }

    @Transactional
    fun updateSettings(userId: Long, dto: SettingsDto): SettingsDto {
        // Таймзону валидируем как зону IANA — иначе бот потом не сможет строить расписание.
        val zone = try {
            ZoneId.of(dto.timezone)
        } catch (_: Exception) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Неизвестная таймзона: ${dto.timezone}")
        }
        if (zone.id != dto.timezone) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Таймзона должна быть в формате IANA, например Europe/Moscow")
        }

        val settings = ensure(userId)
        settings.timezone = zone.id
        settings.reminderTime = LocalTime.of(dto.reminderHour, dto.reminderMinute)
        settings.remindersEnabled = dto.remindersEnabled
        settings.updatedAt = Instant.now()
        return settingsRepository.save(settings).toDto()
    }

    /** Настройки создаются при первом обращении с дефолтами — отдельного «онбординга» нет. */
    private fun ensure(userId: Long): UserSettings =
        settingsRepository.findById(userId).orElseGet { settingsRepository.save(UserSettings(userId = userId)) }

    private fun notFound() = ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден")

    private fun UserSettings.toDto() = SettingsDto(
        timezone = timezone,
        reminderHour = reminderTime.hour,
        reminderMinute = reminderTime.minute,
        remindersEnabled = remindersEnabled,
    )
}
