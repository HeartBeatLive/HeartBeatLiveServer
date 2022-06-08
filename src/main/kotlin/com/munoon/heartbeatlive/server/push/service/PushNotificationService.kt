package com.munoon.heartbeatlive.server.push.service

import com.munoon.heartbeatlive.server.push.PushNotification
import com.munoon.heartbeatlive.server.push.PushNotificationData
import com.munoon.heartbeatlive.server.push.PushNotificationLocale
import com.munoon.heartbeatlive.server.push.model.SendPushNotificationData
import com.munoon.heartbeatlive.server.push.repository.PushNotificationRepository
import com.munoon.heartbeatlive.server.push.sender.push.PushNotificationSender
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import java.util.*

@Service
class PushNotificationService(
    private val sender: PushNotificationSender,
    private val messageSource: MessageSource,
    private val repository: PushNotificationRepository
) {
    suspend fun sendNotifications(vararg notificationsData: PushNotificationData) {
        val notifications = notificationsData.toList()
        sendNotification(notifications)
        saveNotifications(notifications)
    }

    private suspend fun sendNotification(notificationsData: List<PushNotificationData>) {
        notificationsData.forEach { notificationData ->
            val data = notificationData.asSendPushNotificationData(messageSource)
            if (data.userIds.isNotEmpty()) {
                sender.sendNotification(data)
            }
        }
    }

    private suspend fun saveNotifications(notificationsData: List<PushNotificationData>) {
        notificationsData.filter { it.notification != null }
            .flatMap {
                it.userIds.map { userId ->
                    PushNotification(
                        userId = userId,
                        data = it.notification!!
                    )
                }
            }
            .let { if (it.isNotEmpty()) repository.saveAll(it).collect { } }
    }

    private companion object {
        fun PushNotificationData.asSendPushNotificationData(messageSource: MessageSource) = SendPushNotificationData(
            userIds = userIds,
            title = messageSource.getMessages(title),
            content = messageSource.getMessages(content),
            metadata = metadata,
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