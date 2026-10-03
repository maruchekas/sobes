package ru.sobes.botlink

import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.sobes.auth.requireUserId

/** Привязка аккаунта к Telegram-боту. */
@RestController
@RequestMapping("/api/v1/bot-link")
class BotLinkController(private val service: BotLinkService) {

    /** Гасит токен из ссылки бота и связывает аккаунты. */
    @PostMapping("/confirm")
    fun confirm(request: HttpServletRequest, @RequestBody body: ConfirmRequest): BotLinkResult =
        service.confirm(request.requireUserId(), body.token)
}

data class ConfirmRequest(val token: String)
