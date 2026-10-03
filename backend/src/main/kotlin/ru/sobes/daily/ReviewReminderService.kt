package ru.sobes.daily

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class ReviewReminderDto(
    val telegramId: Long,
    val userId: Long,
    val dueCount: Int,
    val oldestDueHours: Long,
)

/**
 * Напоминания о просроченных повторениях (SOBES-23).
 *
 * Отдельно от вопроса дня: повторения — не «один в день», а «сколько накопилось».
 * Пишем в reminder_log, чтобы не спамить: не чаще раза в сутки, пока юзер не
 * разгребёт очередь (если dueCount не уменьшился — повторно не напоминаем).
 */
@Service
class ReviewReminderService(
    private val em: EntityManager,
) {
    @Transactional
    fun nextBatch(limit: Int): List<ReviewReminderDto> {
        val rows = em.createNativeQuery(
            """
            select s.user_id, ta.telegram_id,
                   (select count(*) from review_schedule r where r.user_id = s.user_id and r.due_at <= now()) as due,
                   (select extract(epoch from (now() - min(r.due_at))) / 3600
                      from review_schedule r where r.user_id = s.user_id and r.due_at <= now()) as oldest_h
            from user_settings s
            join telegram_accounts ta on ta.user_id = s.user_id
            where s.reminders_enabled = true
              and (now() at time zone s.timezone)::time >= s.reminder_time
              and exists (select 1 from review_schedule r
                          where r.user_id = s.user_id and r.due_at <= now())
              and not exists (select 1 from reminder_log rl
                              where rl.user_id = s.user_id
                                and rl.kind = 'REVIEW'
                                and rl.sent_at > now() - interval '24 hours')
            order by oldest_h desc
            limit :limit
            """.trimIndent()
        ).setParameter("limit", limit)
            .resultList
            .filterIsInstance<Array<*>>()

        val result = mutableListOf<ReviewReminderDto>()
        for (row in rows) {
            val userId = (row[0] as Number).toLong()
            val telegramId = (row[1] as Number).toLong()
            val dueCount = (row[2] as Number).toInt()
            if (dueCount == 0) continue
            val oldestHours = (row[3] as Number?)?.toLong() ?: 0L

            em.createNativeQuery(
                "insert into reminder_log (user_id, kind, sent_at) values (:uid, 'REVIEW', now())"
            ).setParameter("uid", userId).executeUpdate()

            result.add(
                ReviewReminderDto(
                    telegramId = telegramId,
                    userId = userId,
                    dueCount = dueCount,
                    oldestDueHours = oldestHours,
                )
            )
        }
        return result
    }
}
