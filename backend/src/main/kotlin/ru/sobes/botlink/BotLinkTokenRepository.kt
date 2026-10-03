package ru.sobes.botlink

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface BotLinkTokenRepository : JpaRepository<BotLinkToken, String> {
    fun findFirstByTelegramIdAndUsedAtIsNullOrderByCreatedAtDesc(telegramId: Long): BotLinkToken?
}
