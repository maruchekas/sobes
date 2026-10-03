package ru.sobes.daily

import org.springframework.data.jpa.repository.JpaRepository

interface DailyQuestionRepository : JpaRepository<DailyQuestion, DailyQuestionId> {

    fun countByUserId(userId: Long): Long

    fun findByUserIdOrderByForDateDesc(userId: Long, pageable: org.springframework.data.domain.Pageable): List<DailyQuestion>
}
