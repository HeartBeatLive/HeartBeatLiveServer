package com.munoon.heartbeatlive.server.user.repository

import com.munoon.heartbeatlive.server.AbstractTest
import com.munoon.heartbeatlive.server.config.properties.UserProperties
import io.kotest.common.runBlocking
import io.kotest.matchers.longs.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.instant
import io.kotest.property.arbitrary.ipAddressV4
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
import java.time.Instant

@SpringBootTest
internal class UserResetEmailRequestRepositoryTest : AbstractTest() {
    @Autowired
    private lateinit var repository: UserResetEmailRequestRepository

    @Autowired
    private lateinit var redisConnectionFactory: ReactiveRedisConnectionFactory

    @Autowired
    private lateinit var userProperties: UserProperties

    @Test
    fun `checkIfIpAddressMadeARequest - true`(): Unit = runBlocking {
        checkAll(Arb.ipAddressV4()) { ipAddress ->
            repository.saveANewRequestForIpAddress(ipAddress, Instant.now())
            repository.checkIfIpAddressMadeARequest(ipAddress) shouldBe true
        }
    }

    @Test
    fun `checkIfIpAddressMadeARequest - false`(): Unit = runBlocking {
        checkAll(Arb.ipAddressV4()) { ipAddress ->
            repository.checkIfIpAddressMadeARequest(ipAddress) shouldBe false
        }
    }

    @Test
    fun saveANewRequestForIpAddress(): Unit = runBlocking {
        val redisTemplate = ReactiveRedisTemplate(redisConnectionFactory, RedisSerializationContext.byteArray())

        checkAll(Arb.ipAddressV4(), Arb.instant()) { ipAddress, created ->
            val expectedKey = "userResetEmailRequests:$ipAddress"

            repository.saveANewRequestForIpAddress(ipAddress, created)

            val ttl = userProperties.resetPasswordRequestTimeout.toSeconds()
            redisTemplate.execute { it.keyCommands().ttl(ByteBuffer.wrap(expectedKey.toByteArray())) }
                .awaitSingle() shouldBeInRange (ttl - 3)..ttl

            redisTemplate.opsForValue().getAndAwait(expectedKey.toByteArray()) shouldBe
                    ByteArray(1) { created.epochSecond.toByte() }
        }
    }
}