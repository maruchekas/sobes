package ru.sobes.daily

import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.sobes.auth.TelegramAccountRepository
import ru.sobes.question.QuestionRepository
import java.time.LocalDate
import java.time.ZoneId

data class DailyDigestDto(
    val telegramId: Long,
    val userId: Long,
    val questionId: Long,
    val questionBody: String,
    val answerUrl: String,
)

/**
 * Вопрос дня: бот раз в минуту забирает пачку дайджестов.
 *
 * Кого включать: юзеры с reminders_enabled, у которых локальное время >= reminder_time
 * и на локальную дату ещё не отправлялся вопрос. Ровно один вопрос на юзера в день —
 * PK (user_id, for_date) страхует от дублей при гонке планировщиков.
 */
@Service
class DailyDigestService(
    private val em: EntityManager,
    private val daily: DailyQuestionRepository,
    private val questions: QuestionRepository,
    private val telegramAccounts: TelegramAccountRepository,
    @Value("\${sobes.web-base-url}") private val webBaseUrl: String,
) {
    @Transactional
    fun nextBatch(limit: Int): List<DailyDigestDto> {
        val rows = em.createNativeQuery(
            """
            select s.user_id, ta.telegram_id, s.reminder_time, s.timezone
            from user_settings s
            join telegram_accounts ta on ta.user_id = s.user_id
            where s.reminders_enabled = true
              and (now() at time zone s.timezone)::time >= s.reminder_time
              and not exists (
                    select 1 from daily_questions dq
                    where dq.user_id = s.user_id
                      and dq.for_date = (now() at time zone s.timezone)::date
              )
            order by s.user_id
            limit :limit
            """.trimIndent()
        ).setParameter("limit", limit)
            .resultList
            .filterIsInstance<Array<*>>()

        val result = mutableListOf<DailyDigestDto>()
        for (row in rows) {
            val userId = (row[0] as Number).toLong()
            val telegramId = (row[1] as Number).toLong()
            val questionId = pickQuestion(userId) ?: continue
            val question = questions.findById(questionId).orElse(null) ?: continue

            // Занимаем слот (PK страхует от гонки: дубликат -> исключение -> юзер пропускается).
            try {
                daily.saveAndFlush(
                    DailyQuestion(
                        userId = userId,
                        forDate = LocalDate.now(ZoneId.of(getTimezone(userId))),
                        questionId = questionId,
                    )
                )
            } catch (e: Exception) {
                continue
            }

            result.add(
                DailyDigestDto(
                    telegramId = telegramId,
                    userId = userId,
                    questionId = questionId,
                    questionBody = question.body,
                    answerUrl = "$webBaseUrl/questions/$questionId",
                )
            )
        }
        return result
    }

    /** Незнакомое/новое — приоритет; иначе вопрос с самым старым due. */
    private fun pickQuestion(userId: Long): Long? {
        val newIds = em.createNativeQuery(
            """
            select q.id from questions q
            where q.id not in (select r.question_id from review_schedule r where r.user_id = :uid)
            order by random() limit 1
            """.trimIndent()
        ).setParameter("uid", userId).resultList.firstOrNull() as Number?

        if (newIds != null) return newIds.toLong()

        val dueId = em.createNativeQuery(
            """
            select r.question_id from review_schedule r
            where r.user_id = :uid and r.due_at <= now()
            order by r.due_at asc limit 1
            """.trimIndent()
        ).setParameter("uid", userId).resultList.firstOrNull() as Number?

        if (dueId != null) return dueId.toLong()

        // Всё пройдено и ничего не due — случайный из расписания (поддержание).
        val anyId = em.createNativeQuery(
            "select question_id from review_schedule where user_id = :uid order by random() limit 1"
        ).setParameter("uid", userId).resultList.firstOrNull() as Number?
        return anyId?.toLong()
    }

    private fun getTimezone(userId: Long): String =
        em.createNativeQuery("select timezone from user_settings where user_id = :uid")
            .setParameter("uid", userId)
            .resultList.firstOrNull() as? String ?: "Europe/Moscow"
}
