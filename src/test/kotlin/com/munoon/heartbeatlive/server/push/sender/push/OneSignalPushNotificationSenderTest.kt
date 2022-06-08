package com.munoon.heartbeatlive.server.push.sender.push

import com.munoon.heartbeatlive.server.onesignal.OneSignalClient
import com.munoon.heartbeatlive.server.onesignal.model.OneSignalSendNotification
import com.munoon.heartbeatlive.server.push.PushNotificationLocale
import com.munoon.heartbeatlive.server.push.model.PushNotificationPriority
import com.munoon.heartbeatlive.server.push.model.SendPushNotificationData
import io.kotest.core.spec.style.FreeSpec
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.serialization.json.JsonPrimitive

class OneSignalPushNotificationSenderTest : FreeSpec({
    val client = mockk<OneSignalClient>() {
        coEvery { sendNotification(any()) } returns Unit
    }
    val sender = OneSignalPushNotificationSender(client)

    afterEach { clearMocks(client) }

    "sendNotification" {
        val expectedOneSignalNotification = OneSignalSendNotification(
            contents = mapOf("en" to "english cont", "ru" to "russian cont"),
            headings = mapOf("en" to "english title", "ru" to "russian title"),
            channelForExternalUserIds = OneSignalSendNotification.ChannelForExternalUserIds.PUSH,
            includeExternalUserIds = setOf("userId"),
            data = mapOf(
                "key_1" to JsonPrimitive("value_1"),
                "key_2" to JsonPrimitive(2)
            ),
            priority = 5
        )
        val data = SendPushNotificationData(
            userIds = setOf("userId"),
            title = mapOf(PushNotificationLocale.EN to "english title", PushNotificationLocale.RU to "russian title"),
            content = mapOf(PushNotificationLocale.EN to "english cont", PushNotificationLocale.RU to "russian cont"),
            metadata = mapOf(
                "key_1" to JsonPrimitive("value_1"),
                "key_2" to JsonPrimitive(2)
            ),
            priority = PushNotificationPriority.MEDIUM
        )

        sender.sendNotification(data)

        coVerify { client.sendNotification(expectedOneSignalNotification) }
    }
})
