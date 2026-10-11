package ru.sobes.interview

import org.springframework.data.jpa.repository.JpaRepository

interface InterviewSessionRepository : JpaRepository<InterviewSession, Long> {
    fun findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId: Long, status: String): InterviewSession?
    fun countByUserId(userId: Long): Long
}

interface InterviewAnswerRepository : JpaRepository<InterviewAnswer, Long> {
    fun findBySessionIdOrderByAnsweredAtAsc(sessionId: Long): List<InterviewAnswer>
    fun countBySessionId(sessionId: Long): Int
    fun findBySessionIdAndQuestionId(sessionId: Long, questionId: Long): InterviewAnswer?
}
