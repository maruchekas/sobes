package ru.sobes.auth

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserRepository : JpaRepository<User, Long> {
    @Query(
        "select u from User u join TelegramAccount t on t.userId = u.id " +
            "where t.telegramId = :telegramId"
    )
    fun findByTelegramId(@Param("telegramId") telegramId: Long): User?
}

interface TelegramAccountRepository : JpaRepository<TelegramAccount, Long> {
    fun findByTelegramId(telegramId: Long): TelegramAccount?
    fun findByUserId(userId: Long): TelegramAccount?
}
