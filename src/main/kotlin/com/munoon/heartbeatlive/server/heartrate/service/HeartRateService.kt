package com.munoon.heartbeatlive.server.heartrate.service

import com.munoon.heartbeatlive.server.config.properties.HeartRateStreamProperties
import com.munoon.heartbeatlive.server.heartrate.HeartBeatSubscribersManager
import com.munoon.heartbeatlive.server.heartrate.HeartRateInfoHandler
import com.munoon.heartbeatlive.server.heartrate.HeartRateSubscriber
import com.munoon.heartbeatlive.server.heartrate.TooManyHeartRateSubscriptionsExceptions
import com.munoon.heartbeatlive.server.heartrate.model.HeartRateInfo
import com.munoon.heartbeatlive.server.heartrate.repository.HeartRateSubscriberRepository
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.runBlocking
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class HeartRateService(
    private val repository: HeartRateSubscriberRepository,
    private val subscribersManager: HeartBeatSubscribersManager,
    private val heartRateStreamProperties: HeartRateStreamProperties,
    private val taskExecutor: AsyncTaskExecutor,
    private val handlers: List<HeartRateInfoHandler>
) {
    suspend fun sendHeartRate(userId: String, heartRate: Float) {
        for (handler in handlers) {
            taskExecutor.execute {
                handler.handleHeartRateInfo(userId, heartRate)
            }
        }
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