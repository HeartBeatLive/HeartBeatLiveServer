package com.munoon.heartbeatlive.server.heartrate.handler

import com.munoon.heartbeatlive.server.config.ServerInstanceRunningId
import com.munoon.heartbeatlive.server.heartrate.publisher.HeartRateMessagePublisher
import com.munoon.heartbeatlive.server.messaging.HeartRateMessageOuterClass
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class MessagePublisherHeartRateInfoHandler(
    private val publisher: HeartRateMessagePublisher,
) : HeartRateInfoHandler {
    override suspend fun handleHeartRateInfo(userId: String, heartRate: Float) {
        val message = HeartRateMessageOuterClass.HeartRateMessage.newBuilder()
            .setUserId(userId)
            .setHeartRate(heartRate)
            .setPublishTimeEpochMillis(Instant.now().toEpochMilli())
            .setPublisherId(ServerInstanceRunningId.id)
            .build()

        publisher.publish(message)
    }
}
