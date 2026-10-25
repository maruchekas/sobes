package ru.sobes.observability

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Бизнес-метрики Sobes (SOBES-33): регистрации, ответы на вопросы,
 * интервью, LLM-оценки, действия бота. Красный путь без метрик — слепой.
 */
@Component
class SobesMetrics(registry: MeterRegistry) {

    private val registrations: Counter = Counter.builder("sobes_registrations_total")
        .description("Новые пользователи прошли вход")
        .register(registry)

    private val answers: Counter = Counter.builder("sobes_answers_total")
        .description("Ответы на вопросы практики (SM-2)")
        .register(registry)

    private val answerSeconds: Timer = Timer.builder("sobes_answer_seconds")
        .description("Время на ответ в практике")
        .publishPercentiles(0.5, 0.95)
        .register(registry)

    private val interviews: Counter = Counter.builder("sobes_interviews_total")
        .description("Начатые мок-интервью")
        .register(registry)

    private val llmEvaluations: Counter = Counter.builder("sobes_llm_evaluations_total")
        .description("LLM-оценки ответов")
        .register(registry)

    private val llmFailures: Counter = Counter.builder("sobes_llm_failures_total")
        .description("LLM-оценки недоступны (таймаут/ошибка/нет ключа)")
        .register(registry)

    private val llmSeconds: Timer = Timer.builder("sobes_llm_seconds")
        .description("Длительность LLM-оценки")
        .publishPercentiles(0.5, 0.95)
        .register(registry)

    private val botDailyDelivered: Counter = Counter.builder("sobes_bot_daily_delivered_total")
        .description("Вопросы дня, доставленные ботом")
        .register(registry)

    private val botReminders: Counter = Counter.builder("sobes_bot_reminders_total")
        .description("Напоминания о просроченных повторениях")
        .register(registry)

    fun registration() = registrations.increment()
    fun answer(seconds: Long) {
        answers.increment()
        answerSeconds.record(Duration.ofSeconds(seconds.coerceAtLeast(0)))
    }
    fun interviewStarted() = interviews.increment()
    fun llmEvaluated(seconds: Long) {
        llmEvaluations.increment()
        llmSeconds.record(Duration.ofMillis(seconds))
    }
    fun llmFailed() = llmFailures.increment()
    fun botDaily() = botDailyDelivered.increment()
    fun botReminder() = botReminders.increment()
}
