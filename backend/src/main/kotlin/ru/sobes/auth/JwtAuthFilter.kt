package ru.sobes.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/** JWT-фильтр: Bearer-токен -> атрибут запроса userId. */
@Component
class JwtAuthFilter(private val jwtService: JwtService) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            val userId = jwtService.parse(header.removePrefix("Bearer ").trim())
            if (userId != null) {
                request.setAttribute("userId", userId)
            }
        }
        chain.doFilter(request, response)
    }
}

