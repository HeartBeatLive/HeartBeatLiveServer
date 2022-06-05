package com.munoon.heartbeatlive.server.push.sender.push

import com.munoon.heartbeatlive.server.onesignal.OneSignalClient
import com.munoon.heartbeatlive.server.onesignal.model.OneSignalSendNotification
import com.munoon.heartbeatlive.server.push.model.SendPushNotificationData
import org.springframework.stereotype.Component

@Component
class OneSignalPushNotificationSender(private val oneSignalClient: OneSignalClient) : PushNotificationSender {
    override suspend fun sendNotification(data: SendPushNotificationData) {
        val notification = data.asOneSignalNotification()
        oneSignalClient.sendNotification(notification)
    }

    private companion object {
        fun SendPushNotificationData.asOneSignalNotification() = OneSignalSendNotification(
            contents = content.mapKeys { it.key.language },
            headings = title.mapKeys { it.key.language },
            channelForExternalUserIds = OneSignalSendNotification.ChannelForExternalUserIds.PUSH,
            includeExternalUserIds = setOf(userId),
            data = metadata
        )
    }
}