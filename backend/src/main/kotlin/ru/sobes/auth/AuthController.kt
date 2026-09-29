package ru.sobes.auth

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestHeader

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(private val authService: AuthService) {

    /** Вход через Telegram Login Widget: все поля виджета, включая hash. */
    @PostMapping("/telegram")
    fun telegram(@RequestBody body: Map<String, String>): LoginResponse =
        authService.loginByTelegram(body)

    /** Текущий пользователь по Bearer-токену. */
    @GetMapping("/me")
    fun me(request: HttpServletRequest): UserView =
        authService.currentUser(request.requireUserId())

    @ExceptionHandler(UnauthorizedException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun unauthorized() = mapOf("error" to "unauthorized")
}
