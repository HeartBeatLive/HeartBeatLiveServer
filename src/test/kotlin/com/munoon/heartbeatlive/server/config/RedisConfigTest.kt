package com.munoon.heartbeatlive.server.config

import com.google.protobuf.Message
import com.munoon.heartbeatlive.server.AbstractTest
import com.munoon.heartbeatlive.server.messaging.HeartRateMessageOuterClass.HeartRateMessage
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.ReactiveRedisTemplate
import java.nio.ByteBuffer
import java.time.Instant

@SpringBootTest
internal class RedisConfigTest : AbstractTest() {
    @Autowired
    private lateinit var protobufRedisTemplate: ReactiveRedisTemplate<Message, Message>

    @Test
    fun `protobufRedisTemplate - test serializer`() {
        val message = HeartRateMessage.newBuilder()
            .setUserId("userId")
            .setHeartRate(123.45f)
            .setPublishTimeEpochMillis(Instant.now().toEpochMilli())
            .setPublisherId("publisherId")
            .build()

        val expected = message.toByteArray()
        val actual = protobufRedisTemplate.serializationContext.valueSerializationPair.write(message).array()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `protobufRedisTemplate - test deserializer`() {
        assertThatThrownBy {
            protobufRedisTemplate.serializationContext.valueSerializationPair.read(ByteBuffer.allocate(0))
        }.isExactlyInstanceOf(UnsupportedOperationException::class.java)
    }
}