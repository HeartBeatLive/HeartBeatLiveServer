package com.munoon.heartbeatlive.server.heartrate

import com.munoon.heartbeatlive.server.heartrate.model.HeartRateInfo
import com.munoon.heartbeatlive.server.subscription.service.SubscriptionService
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import java.util.concurrent.ConcurrentHashMap

@Component
class HeartBeatSubscribersManager(private val subscriptionService: SubscriptionService) {
    private val userSubscriptions = ConcurrentHashMap<String, MutableList<FluxSink<HeartRateInfo>>>()

    suspend fun sendHeartRate(userId: String, heartRate: Float) {
        // send to himself
        userSubscriptions[userId]?.forEach { sink ->
            val message = HeartRateInfo(subscriptionId = null, heartRate = heartRate, ownHeartRate = true)
            sink.next(message)
        }

        // send to subscribers
        subscriptionService.getAllSubscriptionsByUserId(userId).collect { // TODO add caching
            userSubscriptions[it.subscriberUserId]?.forEach { sink ->
                val message = HeartRateInfo(subscriptionId = it.id, heartRate = heartRate, ownHeartRate = false)
                sink.next(message)
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
}