package ru.sobes.auth

/** Хелпер для тестов: подписывает параметры Telegram Login Widget тестовым токеном бота. */
object TestTelegramAuth {

    const val BOT_TOKEN = "123456:TEST-token-for-integration-tests"

    fun signedWidgetParams(
        id: Long = 555777L,
        firstName: String = "Тест",
        lastName: String = "Тестов",
        username: String = "test_user",
        authDate: Long = System.currentTimeMillis() / 1000,
    ): Map<String, String> {
        val fields = linkedMapOf(
            "auth_date" to authDate.toString(),
            "first_name" to firstName,
            "id" to id.toString(),
            "last_name" to lastName,
            "username" to username,
        )
        val checkString = fields.entries.sortedWith(compareBy { it.key })
            .joinToString("\n") { "${it.key}=${it.value}" }
        val secret = java.security.MessageDigest.getInstance("SHA-256")
            .digest(BOT_TOKEN.toByteArray())
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(javax.crypto.spec.SecretKeySpec(secret, "HmacSHA256"))
        val hash = mac.doFinal(checkString.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return fields + mapOf("hash" to hash)
    }
}
