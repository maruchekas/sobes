package ru.sobes.bot

import org.slf4j.LoggerFactory

/** Точка входа бота. Переменные: BOT_TOKEN, API_BASE, BOT_INTERNAL_SECRET, DIGEST_INTERVAL_SEC (по умолч. 60). */
fun main() {
    val log = LoggerFactory.getLogger("main")
    val token = System.getenv("BOT_TOKEN")
        ?: error("BOT_TOKEN не задан")
    val apiBase = System.getenv("API_BASE") ?: "http://localhost:8080"
    val botSecret = System.getenv("BOT_INTERNAL_SECRET") ?: error("BOT_INTERNAL_SECRET не задан")
    val digestIntervalSec = System.getenv("DIGEST_INTERVAL_SEC")?.toLongOrNull() ?: 60L

    val api = SobesApi(apiBase, botSecret)
    val bot = BotLogic(TelegramClient(token), api)
    Runtime.getRuntime().addShutdownHook(Thread { bot.stop() })

    // Рассылка «вопрос дня»: backend сам решает, кому пора (таймзоны, время из кабинета).
    val digestThread = Thread {
        while (!Thread.currentThread().isInterrupted) {
            try {
                val digests = api.dailyDigest()
                for (d in digests) {
                    runCatching {
                        bot.sendDailyQuestion(d.telegramId, d.questionBody, d.answerUrl)
                    }.onFailure {
                        log.warn("digest {}: не доставлен ({})", d.telegramId, it.message)
                    }
                }
                if (digests.isNotEmpty()) log.info("digest: доставлено {}", digests.size)
            } catch (e: Exception) {
                log.warn("digest poll failed: {}", e.message)
            }
            Thread.sleep(digestIntervalSec * 1000)
        }
    }
    digestThread.isDaemon = true
    digestThread.start()

    log.info("Sobes bot: api=$apiBase, digest каждые ${digestIntervalSec}s")
    bot.start()
}
