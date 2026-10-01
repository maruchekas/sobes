package ru.sobes.dashboard

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import java.time.LocalDate

/** Прогресс по категории. */
data class CategoryRow(
    val slug: String,
    val title: String,
    val answered: Long,
    val confidence: Double,
)

data class DayRow(val day: LocalDate, val answers: Long)

/**
 * Нативные SQL-запросы агрегаций дашборда (JPQL не умеет at time zone и case-avg так чисто).
 */
@Repository
class DashboardQueryRepository(private val em: EntityManager) {

    /** Уверенность по категориям: AGAIN=0, HARD=0.55, GOOD=0.8, EASY=1.0. */
    fun progressByCategory(userId: Long): List<CategoryRow> {
        val rows = em.createNativeQuery(
            """
            select c.slug, c.title, count(*) as answered,
                   avg(case ua.self_rating
                         when 'AGAIN' then 0.0
                         when 'HARD'  then 0.55
                         when 'GOOD'  then 0.8
                         when 'EASY'  then 1.0 end) as confidence
            from user_answers ua
                join questions q on q.id = ua.question_id
                join categories c on c.id = q.category_id
            where ua.user_id = :userId
            group by c.slug, c.title
            order by confidence asc
            """
        ).setParameter("userId", userId).resultList
        return rows.map { r ->
            @Suppress("UNCHECKED_CAST")
            val row = r as Array<Any>
            CategoryRow(
                slug = row[0].toString(),
                title = row[1].toString(),
                answered = (row[2] as Number).toLong(),
                confidence = (row[3] as Number).toDouble(),
            )
        }
    }

    /** Ответы по дням в таймзоне пользователя — сырьё для streak. */
    fun answersByDay(userId: Long, tz: String): List<DayRow> {
        val rows = em.createNativeQuery(
            """
            select (ua.answered_at at time zone :tz)::date as day, count(*) as answers
            from user_answers ua
            where ua.user_id = :userId
            group by day
            order by day desc
            """
        ).setParameter("userId", userId)
            .setParameter("tz", tz)
            .resultList
        return rows.map { r ->
            @Suppress("UNCHECKED_CAST")
            val row = r as Array<Any>
            DayRow(
                day = row[0].toString().let { LocalDate.parse(it) },
                answers = (row[1] as Number).toLong(),
            )
        }
    }
}
