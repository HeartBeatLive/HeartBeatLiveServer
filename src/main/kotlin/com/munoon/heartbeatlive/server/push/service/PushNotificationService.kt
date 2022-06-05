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
    suspend fun sendNotification(notificationData: PushNotificationData) {
        val data = notificationData.asSendPushNotificationData(messageSource)
        sender.sendNotification(data)

        notificationData.notification
            ?.let { notification -> repository.save(PushNotification(
                userId = notificationData.userId,
                data = notification
            )) }
    }

    private companion object {
        fun PushNotificationData.asSendPushNotificationData(messageSource: MessageSource) = SendPushNotificationData(
            userId = userId,
            title = messageSource.getMessages(title),
            content = messageSource.getMessages(content),
            metadata = metadata
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