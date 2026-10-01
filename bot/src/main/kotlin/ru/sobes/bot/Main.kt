package ru.sobes.bot

import org.slf4j.LoggerFactory

/** Точка входа бота. Переменные: BOT_TOKEN, API_BASE (http://localhost:8080). */
fun main() {
    val log = LoggerFactory.getLogger("main")
    val token = System.getenv("BOT_TOKEN")
        ?: error("BOT_TOKEN не задан")
    val apiBase = System.getenv("API_BASE") ?: "http://localhost:8080"

    val bot = BotLogic(TelegramClient(token), SobesApi(apiBase))
    Runtime.getRuntime().addShutdownHook(Thread { bot.stop() })
    log.info("Sobes bot: api=$apiBase")
    bot.start()
}
