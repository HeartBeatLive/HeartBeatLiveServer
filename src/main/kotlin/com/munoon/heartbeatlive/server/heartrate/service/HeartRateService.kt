package com.munoon.heartbeatlive.server.heartrate.service

import com.munoon.heartbeatlive.server.config.ServerInstanceRunningId
import com.munoon.heartbeatlive.server.config.properties.HeartRateStreamProperties
import com.munoon.heartbeatlive.server.heartrate.HeartBeatSubscribersManager
import com.munoon.heartbeatlive.server.heartrate.HeartRateSubscriber
import com.munoon.heartbeatlive.server.heartrate.TooManyHeartRateSubscriptionsExceptions
import com.munoon.heartbeatlive.server.heartrate.model.HeartRateInfo
import com.munoon.heartbeatlive.server.heartrate.publisher.HeartRateMessagePublisher
import com.munoon.heartbeatlive.server.heartrate.repository.HeartRateSubscriberRepository
import com.munoon.heartbeatlive.server.messaging.HeartRateMessageOuterClass.HeartRateMessage
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.runBlocking
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import java.time.Instant

@Service
class HeartRateService(
    private val repository: HeartRateSubscriberRepository,
    private val subscribersManager: HeartBeatSubscribersManager,
    private val publisher: HeartRateMessagePublisher,
    private val heartRateStreamProperties: HeartRateStreamProperties,
    private val taskExecutor: AsyncTaskExecutor
) {
    suspend fun sendHeartRate(userId: String, heartRate: Float) {
        // publishing message to other servers
        taskExecutor.execute {
            val message = HeartRateMessage.newBuilder()
                .setUserId(userId)
                .setHeartRate(heartRate)
                .setPublishTimeEpochMillis(Instant.now().toEpochMilli())
                .setPublisherId(ServerInstanceRunningId.id)
                .build()
            runBlocking { publisher.publish(message) }
        }

        // sending to same server subscribers
        taskExecutor.execute { runBlocking {
            subscribersManager.sendHeartRate(userId, heartRate)
        } }
    }

    fun subscribeToHeartRates(userId: String): Flux<HeartRateInfo> {
        return mono {
            // validate user's subscriptions count
            val count = repository.countAllByUserId(userId)
            if (count + 1 > heartRateStreamProperties.subscriptionsCountLimitPerUser) {
                throw TooManyHeartRateSubscriptionsExceptions(userId, heartRateStreamProperties.subscriptionsCountLimitPerUser)
            }

            // save new user subscription
            repository.save(HeartRateSubscriber(userId = userId))
        }.flatMapMany { subscription ->
            subscribersManager.createSubscription(userId)
                .doOnCancel { runBlocking { repository.delete(subscription) } } // delete subscription from database
        }
    }
}