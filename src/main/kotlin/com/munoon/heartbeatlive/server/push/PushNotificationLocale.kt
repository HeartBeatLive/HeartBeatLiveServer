package com.munoon.heartbeatlive.server.push

import java.util.*

object PushNotificationLocale {
    val EN = Locale("en")
    val RU = Locale("ru")

    val ALL_LOCALES_LIST = listOf(EN, RU)

    fun Locale.asPushNotificationLocale(): Locale = when (this.language) {
        "en" -> EN
        "ru" -> RU
        else -> EN
    }
}