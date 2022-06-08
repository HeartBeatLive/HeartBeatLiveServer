package com.munoon.heartbeatlive.server.heartrate

import com.munoon.heartbeatlive.server.config.ServerInstanceRunningId
import com.munoon.heartbeatlive.server.config.properties.HeartRateStreamProperties
import com.munoon.heartbeatlive.server.heartrate.publisher.HeartRateMessagePublisher
import com.munoon.heartbeatlive.server.heartrate.repository.HighLowHeartRateNotificationRepository
import com.munoon.heartbeatlive.server.messaging.HeartRateMessageOuterClass
import com.munoon.heartbeatlive.server.push.HighHeartRateNotificationData
import com.munoon.heartbeatlive.server.push.HighOwnHeartRateNotificationData
import com.munoon.heartbeatlive.server.push.LowHeartRateNotificationData
import com.munoon.heartbeatlive.server.push.LowOwnHeartRateNotificationData
import com.munoon.heartbeatlive.server.push.PushNotificationData
import com.munoon.heartbeatlive.server.push.service.PushNotificationService
import com.munoon.heartbeatlive.server.subscription.service.UserSubscribersLoaderService
import com.munoon.heartbeatlive.server.user.service.UserService
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import java.time.Instant

interface HeartRateInfoHandler {
    fun handleHeartRateInfo(userId: String, heartRate: Float)
    fun filter(userId: String, heartRate: Float): Boolean = true
}

@Component
class MessagePublisherHeartRateInfoHandler(
    private val publisher: HeartRateMessagePublisher,
) : HeartRateInfoHandler {
    override fun handleHeartRateInfo(userId: String, heartRate: Float) {
        val message = HeartRateMessageOuterClass.HeartRateMessage.newBuilder()
            .setUserId(userId)
            .setHeartRate(heartRate)
            .setPublishTimeEpochMillis(Instant.now().toEpochMilli())
            .setPublisherId(ServerInstanceRunningId.id)
            .build()

        runBlocking { publisher.publish(message) }
    }
}

@Component
class LocalSubscribersSenderHeartRateInfoHandler(
    private val subscribersManager: HeartBeatSubscribersManager,
) : HeartRateInfoHandler {
    override fun handleHeartRateInfo(userId: String, heartRate: Float) {
        runBlocking { subscribersManager.sendHeartRate(userId, heartRate) }
    }
}

@Component
class UserStatusUpdaterHeartRateInfoHandler(
    private val userService: UserService
) : HeartRateInfoHandler {
    override fun handleHeartRateInfo(userId: String, heartRate: Float) {
        runBlocking { userService.updateUserLastHeartRateReceiveTime(userId, Instant.now()) }
    }
}

@Component
class BigOrLowHeartRateDetectorInfoHandler(
    private val pushNotificationService: PushNotificationService,
    private val userSubscribersLoaderService: UserSubscribersLoaderService,
    private val userService: UserService,
    private val highLowHeartRateNotificationRepository: HighLowHeartRateNotificationRepository,
    private val heartRateStreamProperties: HeartRateStreamProperties
) : HeartRateInfoHandler {
    override fun filter(userId: String, heartRate: Float): Boolean {
        val minNormalHeartRate = heartRateStreamProperties.highLowPush.normalHeartRate.min
        val maxNormalHeartRate = heartRateStreamProperties.highLowPush.normalHeartRate.max
        return heartRate !in minNormalHeartRate..maxNormalHeartRate
    }

    override fun handleHeartRateInfo(userId: String, heartRate: Float) = runBlocking {
        if (highLowHeartRateNotificationRepository.existsByUserId(userId)) {
            return@runBlocking
        }
        val sendPushTimeoutDuration = heartRateStreamProperties.highLowPush.sendPushTimeoutDuration
        highLowHeartRateNotificationRepository.saveForUserId(userId, Instant.now(), sendPushTimeoutDuration)

        val subscribersUserIds = userSubscribersLoaderService.load(userId).values
        val heartRateOwnerUserDisplayName = userService.getUserById(userId).displayName ?: "User"

        val notificationData: PushNotificationData
        val heartRateOwnerNotificationData: PushNotificationData
        when {
            heartRate > heartRateStreamProperties.highLowPush.normalHeartRate.max -> {
                notificationData = HighHeartRateNotificationData(
                    heartRate = heartRate,
                    heartRateOwnerUserId = userId,
                    heartRateOwnerUserDisplayName = heartRateOwnerUserDisplayName,
                    userIds = subscribersUserIds.toSet()
                )
                heartRateOwnerNotificationData = HighOwnHeartRateNotificationData(
                    heartRate = heartRate,
                    userId = userId
                )
            }
            heartRate < heartRateStreamProperties.highLowPush.normalHeartRate.min -> {
                notificationData = LowHeartRateNotificationData(
                    heartRate = heartRate,
                    heartRateOwnerUserId = userId,
                    heartRateOwnerUserDisplayName = heartRateOwnerUserDisplayName,
                    userIds = subscribersUserIds.toSet()
                )
                heartRateOwnerNotificationData = LowOwnHeartRateNotificationData(
                    heartRate = heartRate,
                    userId = userId
                )
            }
            else -> throw IllegalArgumentException("Heart rate is normal")
        }

        pushNotificationService.sendNotifications(notificationData, heartRateOwnerNotificationData)
    }
}