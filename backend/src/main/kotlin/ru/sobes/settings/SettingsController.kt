package ru.sobes.settings

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.sobes.auth.requireUserId

/** Личный кабинет: профиль + настройки напоминаний. */
@RestController
@RequestMapping("/api/v1/me")
class SettingsController(private val service: SettingsService) {

    @GetMapping("/settings")
    fun cabinet(request: HttpServletRequest): CabinetDto = service.getCabinet(request.requireUserId())

    @PutMapping("/settings")
    fun update(request: HttpServletRequest, @Valid @RequestBody dto: SettingsDto): SettingsDto =
        service.updateSettings(request.requireUserId(), dto)
}
