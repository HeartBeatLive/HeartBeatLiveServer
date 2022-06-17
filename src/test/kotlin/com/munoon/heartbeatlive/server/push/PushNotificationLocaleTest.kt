package com.munoon.heartbeatlive.server.push

import com.munoon.heartbeatlive.server.push.PushNotificationLocale.asPushNotificationLocale
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.spec.style.freeSpec
import io.kotest.matchers.shouldBe
import java.util.*

class PushNotificationLocaleTest : FreeSpec({
    include(asPushNotificationLocaleTest("ru", PushNotificationLocale.RU))
    include(asPushNotificationLocaleTest("en", PushNotificationLocale.EN))
    include(asPushNotificationLocaleTest("unknown", PushNotificationLocale.EN))
})

fun asPushNotificationLocaleTest(localeLanguage: String, expectedLocale: Locale) = freeSpec {
    "asPushNotificationLocale - locale with language '$localeLanguage' should return $expectedLocale" {
        Locale(localeLanguage).asPushNotificationLocale() shouldBe expectedLocale
    }
}