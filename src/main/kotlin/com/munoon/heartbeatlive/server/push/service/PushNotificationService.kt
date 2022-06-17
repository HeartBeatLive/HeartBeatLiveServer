package com.munoon.heartbeatlive.server.push.service

import com.munoon.heartbeatlive.server.common.PageResult
import com.munoon.heartbeatlive.server.push.PushNotification
import com.munoon.heartbeatlive.server.push.PushNotificationData
import com.munoon.heartbeatlive.server.push.PushNotificationLocale
import com.munoon.heartbeatlive.server.push.PushNotificationMapper.getMessageText
import com.munoon.heartbeatlive.server.push.PushNotificationMessage
import com.munoon.heartbeatlive.server.push.PushNotificationNotFoundByIdException
import com.munoon.heartbeatlive.server.push.model.SendPushNotificationData
import com.munoon.heartbeatlive.server.push.repository.PushNotificationRepository
import com.munoon.heartbeatlive.server.push.sender.PushNotificationSender
import kotlinx.coroutines.reactive.asFlow
import kotlinx.serialization.json.JsonPrimitive
import org.slf4j.LoggerFactory
import org.springframework.context.MessageSource
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.*

@Service
class PushNotificationService(
    private val sender: PushNotificationSender,
    private val messageSource: MessageSource,
    private val repository: PushNotificationRepository
) {
    private val logger = LoggerFactory.getLogger(PushNotificationService::class.java)

    suspend fun sendNotifications(notificationsData: List<PushNotificationData>) {
        val sendPushNotifications = arrayListOf<SendPushNotificationData>()
        val pushNotifications = arrayListOf<PushNotification>()

        for (notificationData in notificationsData) {
            val notificationTitle = messageSource.getMessages(notificationData.message.title)
            val notificationContent = messageSource.getMessages(notificationData.message.content)

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
            try {
                sender.sendNotification(pushNotification)
            } catch (e: Exception) {
                logger.error("Error when sending push notification to users ${pushNotification.userIds}", e)
            }
        }
    }

    suspend fun getPushNotificationsByUserId(userId: String, pageable: Pageable): PageResult<PushNotification> =
        PageResult(
            data = repository.findAllByUserId(userId, pageable).asFlow(),
            totalItemsCount = repository.countAllByUserId(userId)
        )

    suspend fun getPushNotificationById(id: String) = repository.findById(id)
        ?: throw PushNotificationNotFoundByIdException(id)

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

        fun MessageSource.getMessages(message: PushNotificationMessage.Message): Map<Locale, String> =
            PushNotificationLocale.ALL_LOCALES_LIST.associateWith { getMessageText(message, it) }
    }
}

suspend fun PushNotificationService.sendNotifications(vararg notificationsData: PushNotificationData) {
    sendNotifications(notificationsData.toList())
}