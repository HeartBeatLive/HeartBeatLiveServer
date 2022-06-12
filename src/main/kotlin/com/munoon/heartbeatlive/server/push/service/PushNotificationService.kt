package com.munoon.heartbeatlive.server.push.service

import com.munoon.heartbeatlive.server.push.PushNotification
import com.munoon.heartbeatlive.server.push.PushNotificationData
import com.munoon.heartbeatlive.server.push.PushNotificationLocale
import com.munoon.heartbeatlive.server.push.model.SendPushNotificationData
import com.munoon.heartbeatlive.server.push.repository.PushNotificationRepository
import com.munoon.heartbeatlive.server.push.sender.push.PushNotificationSender
import kotlinx.serialization.json.JsonPrimitive
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import java.util.*

@Service
class PushNotificationService(
    private val sender: PushNotificationSender,
    private val messageSource: MessageSource,
    private val repository: PushNotificationRepository
) {
    suspend fun sendNotifications(notificationsData: List<PushNotificationData>) {
        val sendPushNotifications = arrayListOf<SendPushNotificationData>()
        val pushNotifications = arrayListOf<PushNotification>()

        for (notificationData in notificationsData) {
            val notificationTitle = messageSource.getMessages(notificationData.title)
            val notificationContent = messageSource.getMessages(notificationData.content)

            if (notificationData.notification == null) {
                sendPushNotifications += notificationData.userIds.map {
                    notificationData.asSendPushNotificationData(
                        notificationTitle,
                        notificationContent,
                        userId = it,
                        notificationId = null
                    )
                }
                continue
            }

            for (userId in notificationData.userIds) {
                val pushNotification = PushNotification(
                    userId = userId,
                    data = notificationData.notification!!
                )
                pushNotifications += pushNotification
                sendPushNotifications += notificationData.asSendPushNotificationData(
                    notificationTitle, notificationContent,
                    userId, pushNotification.id
                )
            }
        }

        if (pushNotifications.isNotEmpty()) {
            repository.saveAll(pushNotifications).collect { }
        }

        for (pushNotification in sendPushNotifications) {
            sender.sendNotification(pushNotification)
        }
    }

    private companion object {
        const val NOTIFICATION_ID_METADATA_KEY = "heart_beat_live:notification_id"

        fun PushNotificationData.asSendPushNotificationData(
            title: Map<Locale, String>,
            content: Map<Locale, String>,
            userId: String,
            notificationId: String?
        ) = SendPushNotificationData(
            userIds = setOf(userId),
            title = title,
            content = content,
            metadata = notificationId?.let { mapOf(NOTIFICATION_ID_METADATA_KEY to JsonPrimitive(notificationId)) }
                ?: emptyMap(),
            priority = priority
        )

        fun MessageSource.getMessages(message: PushNotificationData.Message): Map<Locale, String> {
            val code = message.code
            val arguments = message.arguments.toTypedArray()
            return mapOf(
                PushNotificationLocale.EN to getMessage(code, arguments, Locale.ROOT),
                PushNotificationLocale.RU to getMessage(code, arguments, PushNotificationLocale.RU)
            )
        }
    }
}

suspend fun PushNotificationService.sendNotifications(vararg notificationsData: PushNotificationData) {
    sendNotifications(notificationsData.toList())
}