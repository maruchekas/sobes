package ru.sobes.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/** CORS для браузерных вызовов API (фронт на отдельном-origin в dev и проде). */
@Configuration
class CorsConfig(
    // Локальная разработка: wisereport.online (реальный домен проекта) резолвится через hosts на 127.0.0.1.
    @Value("\${sobes.cors.allowed-origins:http://localhost:3000,http://wisereport.online:3000}") private val allowedOrigins: String,
) {
    @Bean
    fun corsConfigurer(): WebMvcConfigurer = object : WebMvcConfigurer {
        override fun addCorsMappings(registry: CorsRegistry) {
            registry.addMapping("/api/**")
                .allowedOrigins(*allowedOrigins.split(",").map { it.trim() }.toTypedArray())
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .maxAge(3600)
        }
    }
}
