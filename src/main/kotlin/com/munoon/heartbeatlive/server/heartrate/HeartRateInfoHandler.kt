package com.munoon.heartbeatlive.server.heartrate

import com.munoon.heartbeatlive.server.config.ServerInstanceRunningId
import com.munoon.heartbeatlive.server.heartrate.publisher.HeartRateMessagePublisher
import com.munoon.heartbeatlive.server.messaging.HeartRateMessageOuterClass
import com.munoon.heartbeatlive.server.user.service.UserService
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import java.time.Instant

interface HeartRateInfoHandler {
    fun handleHeartRateInfo(userId: String, heartRate: Float)
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