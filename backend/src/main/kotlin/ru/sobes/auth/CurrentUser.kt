package ru.sobes.auth

import jakarta.servlet.http.HttpServletRequest

/** Достаёт id аутентифицированного пользователя (атрибут от JwtAuthFilter) или кидает 401. */
fun HttpServletRequest.requireUserId(): Long =
    getAttribute("userId") as? Long
        ?: throw UnauthorizedException("Требуется авторизация")

class UnauthorizedException(message: String) : RuntimeException(message)
