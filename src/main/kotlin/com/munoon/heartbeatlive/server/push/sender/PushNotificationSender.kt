package com.munoon.heartbeatlive.server.push.sender

import com.munoon.heartbeatlive.server.push.model.SendPushNotificationData

interface PushNotificationSender {
    suspend fun sendNotification(data: SendPushNotificationData)
}