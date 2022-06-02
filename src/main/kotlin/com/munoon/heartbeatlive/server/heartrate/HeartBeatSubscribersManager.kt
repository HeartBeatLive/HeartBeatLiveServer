package com.munoon.heartbeatlive.server.heartrate

import com.munoon.heartbeatlive.server.heartrate.model.HeartRateInfo
import com.munoon.heartbeatlive.server.subscription.service.UserSubscribersLoaderService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import java.util.concurrent.ConcurrentHashMap

@Component
class HeartBeatSubscribersManager(private val userSubscribersLoaderService: UserSubscribersLoaderService) {
    private val userSubscriptions = ConcurrentHashMap<String, MutableList<FluxSink<HeartRateInfo>>>()
    private val logger = LoggerFactory.getLogger(HeartBeatSubscribersManager::class.java)

    fun sendHeartRate(userId: String, heartRate: Float) {
        sendHeartRateToYourself(userId, heartRate)
        sendHeartRateToSubscribers(userId, heartRate)
    }

    private fun sendHeartRateToYourself(userId: String, heartRate: Float) {
        userSubscriptions[userId]?.safeForEach(
            { logger.error("Error sending heart rate to user '$userId' (own heart rate, global error)", it) }
        ) { sink ->
            val message = HeartRateInfo(subscriptionId = null, heartRate = heartRate, ownHeartRate = true)
            try {
                sink.next(message)
            } catch (e: Exception) {
                logger.error("Error sending heart rate to user '$userId' (own heart rate, sink error)", e)
            }
        }
    }

    private fun sendHeartRateToSubscribers(userId: String, heartRate: Float) {
        userSubscribersLoaderService.load(userId).safeForEach(
            { logger.error("Error sending heart rate to user '$userId' (subscriber, global error)", it) }
        ) { (subscriptionId, subscriberUserId) ->
            userSubscriptions[subscriberUserId]?.safeForEach(
                { logger.error("Error sending heart rate to user '$userId' (subscriber, global error)", it) }
            ) { sink ->
                try {
                    val heartRateInfo = HeartRateInfo(
                        subscriptionId = subscriptionId,
                        heartRate = heartRate,
                        ownHeartRate = false
                    )
                    sink.next(heartRateInfo)
                } catch (e: Exception) {
                    logger.error("Error sending heart rate to user '$userId' (subscriber, sink error)", e)
                }
            }
        }
    }

    fun createSubscription(userId: String): Flux<HeartRateInfo> {
        return Flux.create { sink ->
            userSubscriptions.computeIfAbsent(userId) { ArrayList() } += sink
            sink.onCancel {
                userSubscriptions[userId]?.let { subscriptions ->
                    if (subscriptions.size == 1 && subscriptions.first() === sink) {
                        userSubscriptions.remove(userId)
                    } else {
                        subscriptions.removeIf { sink === it }
                    }
                }
            }
        }
    }

    private companion object {
        fun <T> Iterable<T>.safeForEach(exceptionHandler: (Throwable) -> Unit, itemHandler: (T) -> Unit) {
            forEach {
                try {
                    itemHandler(it)
                } catch (e: Throwable) {
                    exceptionHandler(e)
                }
            }
        }

        fun <K, V> Map<K, V>.safeForEach(
            exceptionHandler: (Throwable) -> Unit,
            itemHandler: (Map.Entry<K, V>) -> Unit
        ) {
            forEach {
                try {
                    itemHandler(it)
                } catch (e: Throwable) {
                    exceptionHandler(e)
                }
            }
        }
    }
}