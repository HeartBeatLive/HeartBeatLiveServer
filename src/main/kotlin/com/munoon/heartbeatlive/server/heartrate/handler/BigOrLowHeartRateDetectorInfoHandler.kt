package com.munoon.heartbeatlive.server.heartrate.handler

import com.munoon.heartbeatlive.server.config.properties.HeartRateStreamProperties
import com.munoon.heartbeatlive.server.heartrate.repository.HighLowHeartRateNotificationRepository
import com.munoon.heartbeatlive.server.push.HighHeartRatePushNotificationData
import com.munoon.heartbeatlive.server.push.HighOwnHeartRatePushNotificationData
import com.munoon.heartbeatlive.server.push.LowHeartPushRateNotificationData
import com.munoon.heartbeatlive.server.push.LowOwnHeartRatePushNotificationData
import com.munoon.heartbeatlive.server.push.PushNotificationData
import com.munoon.heartbeatlive.server.push.service.PushNotificationService
import com.munoon.heartbeatlive.server.push.service.sendNotifications
import com.munoon.heartbeatlive.server.subscription.service.UserSubscribersLoaderService
import com.munoon.heartbeatlive.server.user.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class BigOrLowHeartRateDetectorInfoHandler(
    private val pushNotificationService: PushNotificationService,
    private val userSubscribersLoaderService: UserSubscribersLoaderService,
    private val userService: UserService,
    private val highLowHeartRateNotificationRepository: HighLowHeartRateNotificationRepository,
    private val heartRateStreamProperties: HeartRateStreamProperties,
) : HeartRateInfoHandler {
    private val logger = LoggerFactory.getLogger(BigOrLowHeartRateDetectorInfoHandler::class.java)

    override fun filter(userId: String, heartRate: Float): Boolean {
        val minNormalHeartRate = heartRateStreamProperties.highLowPush.normalHeartRate.min
        val maxNormalHeartRate = heartRateStreamProperties.highLowPush.normalHeartRate.max
        return heartRate !in minNormalHeartRate..maxNormalHeartRate
    }

    override suspend fun handleHeartRateInfo(userId: String, heartRate: Float) {
        if (highLowHeartRateNotificationRepository.existsByUserId(userId)) {
            return
        }
        val sendPushTimeoutDuration = heartRateStreamProperties.highLowPush.sendPushTimeoutDuration
        highLowHeartRateNotificationRepository.saveForUserId(userId, Instant.now(), sendPushTimeoutDuration)
        logger.info("Sending user '$userId' high/low heart rate push notification")

        val subscribersUserIds = userSubscribersLoaderService.load(userId).values
        val heartRateOwnerUserDisplayName = userService.getUserById(userId).displayName

        val notificationData: PushNotificationData
        val heartRateOwnerNotificationData: PushNotificationData
        when {
            heartRate > heartRateStreamProperties.highLowPush.normalHeartRate.max -> {
                notificationData = HighHeartRatePushNotificationData(
                    heartRate = heartRate,
                    heartRateOwnerUserId = userId,
                    heartRateOwnerUserDisplayName = heartRateOwnerUserDisplayName,
                    userIds = subscribersUserIds.toSet()
                )
                heartRateOwnerNotificationData = HighOwnHeartRatePushNotificationData(
                    heartRate = heartRate,
                    userId = userId
                )
            }
            heartRate < heartRateStreamProperties.highLowPush.normalHeartRate.min -> {
                notificationData = LowHeartPushRateNotificationData(
                    heartRate = heartRate,
                    heartRateOwnerUserId = userId,
                    heartRateOwnerUserDisplayName = heartRateOwnerUserDisplayName,
                    userIds = subscribersUserIds.toSet()
                )
                heartRateOwnerNotificationData = LowOwnHeartRatePushNotificationData(
                    heartRate = heartRate,
                    userId = userId
                )
            }
            else -> throw IllegalArgumentException("Heart rate is normal")
        }

        pushNotificationService.sendNotifications(notificationData, heartRateOwnerNotificationData)
    }
}