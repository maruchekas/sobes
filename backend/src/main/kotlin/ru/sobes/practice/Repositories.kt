package ru.sobes.practice

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface ReviewScheduleRepository : JpaRepository<ReviewSchedule, ReviewScheduleId> {

    @Query(
        "select r from ReviewSchedule r where r.userId = :userId and r.dueAt <= :now " +
            "order by r.dueAt asc"
    )
    fun findDue(@Param("userId") userId: Long, @Param("now") now: Instant, pageable: Pageable): List<ReviewSchedule>

    /** Новые для пользователя вопросы (ещё без записи в расписании). */
    @Query(
        "select q.id from Question q where q.id not in " +
            "(select r.questionId from ReviewSchedule r where r.userId = :userId) order by q.id"
    )
    fun findNewQuestionIds(@Param("userId") userId: Long, pageable: Pageable): List<Long>

    @Query("select count(r) from ReviewSchedule r where r.userId = :userId and r.dueAt <= :now")
    fun countDue(@Param("userId") userId: Long, @Param("now") now: Instant): Int

    @Query("select count(r) from ReviewSchedule r where r.userId = :userId")
    fun countStarted(@Param("userId") userId: Long): Long
}

interface UserAnswerRepository : JpaRepository<UserAnswer, Long> {
    fun countByUserId(userId: Long): Long
    fun findByUserIdOrderByAnsweredAtDesc(userId: Long, pageable: Pageable): List<UserAnswer>
}
