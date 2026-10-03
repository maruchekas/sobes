package ru.sobes.dashboard

import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.sobes.auth.requireUserId

/** Дашборд прогресса: слабые темы, streak, готовность. */
@RestController
@RequestMapping("/api/v1/dashboard")
class DashboardController(private val service: DashboardService) {

    @GetMapping
    fun dashboard(request: HttpServletRequest): DashboardDto = service.dashboard(request.requireUserId())
}
