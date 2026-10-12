package ru.sobes.interview.llm

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Заглушка, когда LLM_API_KEY не задан: отчёт живёт на самооценке + эталоне. */
@Configuration
class LlmConfig {
    @Bean
    @ConditionalOnMissingBean(AnswerEvaluator::class)
    fun noopEvaluator(): AnswerEvaluator = object : AnswerEvaluator {
        override fun evaluate(questionBody: String, referenceAnswer: String, userAnswer: String): Evaluation? = null
    }
}
