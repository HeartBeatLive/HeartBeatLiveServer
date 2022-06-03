package com.munoon.heartbeatlive.server.heartrate

import com.google.protobuf.Message
import com.munoon.heartbeatlive.server.AbstractTest
import com.munoon.heartbeatlive.server.config.ServerInstanceRunningId
import com.munoon.heartbeatlive.server.messaging.HeartRateMessageOuterClass
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.ReactiveRedisTemplate
import java.time.Instant
import java.time.OffsetDateTime

@SpringBootTest
internal class RedisHeartRateMessageListenerTest : AbstractTest() {
    @Autowired
    private lateinit var protobufRedisTemplate: ReactiveRedisTemplate<Message, Message>

    @MockkBean
    private lateinit var heartBeatSubscribersManager: HeartBeatSubscribersManager

    @Test
    fun handleMessage() {
        coEvery { heartBeatSubscribersManager.sendHeartRate(any(), any()) } returns Unit

        val message1 = HeartRateMessageOuterClass.HeartRateMessage.newBuilder()
            .setUserId("userId")
            .setHeartRate(111.11f)
            .setPublishTimeEpochMillis(Instant.now().toEpochMilli())
            .setPublisherId("publisherId")
            .build()

        val message2 = HeartRateMessageOuterClass.HeartRateMessage.newBuilder()
            .setUserId("userId")
            .setHeartRate(222.22f)
            .setPublishTimeEpochMillis(Instant.now().toEpochMilli())
            .setPublisherId(ServerInstanceRunningId.id) // should ignore own messages
            .build()

        val message3 = HeartRateMessageOuterClass.HeartRateMessage.newBuilder()
            .setUserId("userId")
            .setHeartRate(333.33f)
            // should ignore old messages
            .setPublishTimeEpochMillis(OffsetDateTime.now().minusDays(5).toInstant().toEpochMilli())
            .setPublisherId("publisherId")
            .build()

        protobufRedisTemplate.convertAndSend(RedisHeartRateMessageListener.HEART_RATE_CHANNEL, message3)
            .block()

        protobufRedisTemplate.convertAndSend(RedisHeartRateMessageListener.HEART_RATE_CHANNEL, message2)
            .block()

        protobufRedisTemplate.convertAndSend(RedisHeartRateMessageListener.HEART_RATE_CHANNEL, message1)
            .block()

        coVerify(exactly = 1, timeout = 60000) {
            heartBeatSubscribersManager.sendHeartRate("userId", 111.11f)
        }
        coVerify(exactly = 0) {
            heartBeatSubscribersManager.sendHeartRate("userId", 222.22f)
        }
        coVerify(exactly = 0) {
            heartBeatSubscribersManager.sendHeartRate("userId", 333.33f)
        }
    }
}