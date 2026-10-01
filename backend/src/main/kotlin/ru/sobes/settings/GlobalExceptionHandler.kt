package ru.sobes.settings

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import ru.sobes.auth.UnauthorizedException

/** Глобальные HTTP-маппины: 401/400 без stacktrace в ответе, для всех контроллеров. */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun unauthorized() = mapOf("error" to "unauthorized")

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun badRequest(e: IllegalArgumentException) = mapOf("error" to (e.message ?: "bad_request"))
}
