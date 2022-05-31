package com.munoon.heartbeatlive.server.heartrate.publisher

import com.google.protobuf.Message
import com.munoon.heartbeatlive.server.heartrate.RedisHeartRateMessageListener
import com.munoon.heartbeatlive.server.messaging.HeartRateMessageOuterClass
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.sendAndAwait
import org.springframework.stereotype.Component

@Component
class RedisHeartRateMessagePublisherImpl(
    private val protobufRedisTemplate: ReactiveRedisTemplate<Message, Message>
) : HeartRateMessagePublisher {
    override suspend fun publish(message: HeartRateMessageOuterClass.HeartRateMessage) {
        protobufRedisTemplate.sendAndAwait(RedisHeartRateMessageListener.HEART_RATE_CHANNEL, message)
    }
}