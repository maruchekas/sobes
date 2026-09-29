package ru.sobes.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Проверка данных Telegram Login Widget по алгоритму из документации Telegram. */
@Service
class TelegramLoginVerifier(
    @Value("\${sobes.telegram.bot-token:}") private val botToken: String,
) {
    /** Максимальный возраст данных авторизации — 1 час. */
    private val maxAgeSeconds = 3600L

    /**
     * Проверяет подпись и свежесть данных Login Widget.
     * Алгоритм: data_check_string = отсортированные пары key=value без hash,
     * secret = SHA256(bot_token), подпись = HMAC_SHA256(data_check_string, secret).
     */
    fun verify(params: Map<String, String>): TelegramAuth {
        if (botToken.isBlank()) {
            throw IllegalStateException("sobes.telegram.bot-token не настроен")
        }
        val hash = params["hash"] ?: throw IllegalArgumentException("hash отсутствует")
        val checkString = params.entries
            .filter { it.key != "hash" }
            .sortedWith(compareBy { it.key })
            .joinToString("\n") { "${it.key}=${it.value}" }

        val secret = MessageDigestUtil.sha256(botToken.toByteArray(StandardCharsets.UTF_8))
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret, "HmacSHA256"))
        val expected = mac.doFinal(checkString.toByteArray(StandardCharsets.UTF_8)).toHexString()

        if (!expected.equals(hash, ignoreCase = true)) {
            throw IllegalArgumentException("Подпись Telegram не совпадает")
        }

        val authDate = params["auth_date"]?.toLongOrNull()
            ?: throw IllegalArgumentException("auth_date отсутствует")
        val age = System.currentTimeMillis() / 1000 - authDate
        if (age > maxAgeSeconds || age < -maxAgeSeconds) {
            throw IllegalArgumentException("Данные авторизации устарели (старше часа)")
        }

        return TelegramAuth(
            id = params["id"]?.toLongOrNull() ?: throw IllegalArgumentException("id отсутствует"),
            first_name = params["first_name"],
            last_name = params["last_name"],
            username = params["username"],
            photo_url = params["photo_url"],
            auth_date = authDate,
        )
    }
}

internal object MessageDigestUtil {
    fun sha256(bytes: ByteArray): ByteArray = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
}

internal fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }
