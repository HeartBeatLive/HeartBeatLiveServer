package com.munoon.heartbeatlive.server.heartrate.repository

import com.munoon.heartbeatlive.server.AbstractTest
import io.kotest.common.runBlocking
import io.kotest.matchers.longs.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.instant
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.reactive.awaitSingle
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.getAndAwait
import org.springframework.data.redis.serializer.RedisSerializationContext
import java.nio.ByteBuffer
import java.time.Duration
import java.time.Instant

@SpringBootTest
internal class HighLowHeartRateNotificationRepositoryTest : AbstractTest() {
    @Autowired
    private lateinit var repository: HighLowHeartRateNotificationRepository

    @Autowired
    private lateinit var redisConnectionFactory: ReactiveRedisConnectionFactory

    private companion object {
        val userIdArbitrary = Arb.string(minSize = 1, codepoints = Codepoint.Companion.alphanumeric())
    }

    @Test
    fun `existsByUserId - true`(): Unit = runBlocking {
        checkAll(userIdArbitrary) { userId ->
            repository.saveForUserId(userId, Instant.now(), Duration.ofMinutes(10))
            repository.existsByUserId(userId) shouldBe true
        }
    }

    @Test
    fun `existsByUserId - false`(): Unit = runBlocking {
        checkAll(userIdArbitrary) { userId ->
            repository.existsByUserId(userId) shouldBe false
        }
    }

    @Test
    fun saveForUserId(): Unit = runBlocking {
        val redisTemplate = ReactiveRedisTemplate(redisConnectionFactory, RedisSerializationContext.byteArray())

        checkAll(
            userIdArbitrary,
            Arb.instant(),
            Arb.long(min = 60, max = 20 * 60)
        ) { userId, created, ttl ->
            val expectedKey = "highLowHeartRateNotificationUsersReceived:$userId"

            repository.saveForUserId(userId, created, Duration.ofSeconds(ttl))

            redisTemplate.execute { it.keyCommands().ttl(ByteBuffer.wrap(expectedKey.toByteArray())) }
                .awaitSingle() shouldBeInRange (ttl - 3)..ttl

            redisTemplate.opsForValue().getAndAwait(expectedKey.toByteArray()) shouldBe
                    ByteArray(1) { created.epochSecond.toByte() }
        }
    }
}