package ru.sobes.admin

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Service
import ru.sobes.auth.UserRepository
import ru.sobes.auth.requireUserId

/** Проверка роли ADMIN; бросает 403 для остальных. */
@Service
class AdminGuard(private val users: UserRepository) {
    fun requireAdmin(request: HttpServletRequest) {
        val userId = request.requireUserId()
        val user = users.findById(userId).orElse(null) ?: throw Forbidden()
        if (user.role != "ADMIN") throw Forbidden()
    }
}

class Forbidden : RuntimeException("Недостаточно прав")
