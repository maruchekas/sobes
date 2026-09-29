package ru.sobes.auth

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Date

/** Выдача и проверка JWT-сессий (HS256). */
@Service
class JwtService(
    @Value("\${sobes.jwt.secret:}") private val secret: String,
    @Value("\${sobes.jwt.ttl-hours:720}") private val ttlHours: Long,
) {
    private val key by lazy {
        val s = if (secret.isBlank()) {
            // dev-режим без настроенного секрета: генерируем стабильный ключ из имени приложения,
            // чтобы локальный запуск без конфигурации не падал. В проде секрет обязателен.
            "sobes-dev-secret-do-not-use-in-production-0000000000"
        } else secret
        require(s.length >= 32) { "sobes.jwt.secret должен быть не короче 32 символов" }
        Keys.hmacShaKeyFor(s.toByteArray(Charsets.UTF_8))
    }

    fun issue(userId: Long): String = Jwts.builder()
        .subject(userId.toString())
        .issuedAt(Date.from(Instant.now()))
        .expiration(Date.from(Instant.now().plusSeconds(ttlHours * 3600)))
        .signWith(key)
        .compact()

    /** Возвращает userId или null, если токен невалиден/истёк. */
    fun parse(token: String): Long? = try {
        val claims: Claims = Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).payload
        claims.subject?.toLongOrNull()
    } catch (_: Exception) {
        null
    }
}
