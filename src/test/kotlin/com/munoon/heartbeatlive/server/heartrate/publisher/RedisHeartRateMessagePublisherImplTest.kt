package com.munoon.heartbeatlive.server.heartrate.publisher

import com.google.protobuf.Message
import com.munoon.heartbeatlive.server.heartrate.RedisHeartRateMessageListener
import com.munoon.heartbeatlive.server.messaging.HeartRateMessageOuterClass
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Mono
import java.time.Instant

internal class RedisHeartRateMessagePublisherImplTest {
    @Test
    fun publish() {
        val redisTemplate = mockk<ReactiveRedisTemplate<Message, Message>>()
        every { redisTemplate.convertAndSend(any(), any()) } returns Mono.just(1)

        val message = HeartRateMessageOuterClass.HeartRateMessage.newBuilder()
            .setUserId("userId")
            .setHeartRate(123.45f)
            .setPublishTimeEpochMillis(Instant.now().toEpochMilli())
            .setPublisherId("publisherId")
            .build()

        runBlocking { RedisHeartRateMessagePublisherImpl(redisTemplate).publish(message) }

        verify(exactly = 1) { redisTemplate.convertAndSend(RedisHeartRateMessageListener.HEART_RATE_CHANNEL, message) }
    }
}