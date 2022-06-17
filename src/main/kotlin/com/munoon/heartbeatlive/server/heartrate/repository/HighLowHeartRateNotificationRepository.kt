package com.munoon.heartbeatlive.server.heartrate.repository

import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.getAndAwait
import org.springframework.data.redis.core.setAndAwait
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.Instant

@Repository
class HighLowHeartRateNotificationRepository(redisConnectionFactory: ReactiveRedisConnectionFactory) {
    private val redisTemplate = ReactiveRedisTemplate(redisConnectionFactory, RedisSerializationContext.byteArray())
    private companion object {
        const val COLLECTION_PREFIX = "highLowHeartRateNotificationUsersReceived"
        fun getKey(userId: String) = "$COLLECTION_PREFIX:$userId"
    }

    suspend fun existsByUserId(userId: String): Boolean {
        val key = getKey(userId).toByteArray()
        return redisTemplate.opsForValue().getAndAwait(key) != null
    }

    suspend fun saveForUserId(
        userId: String,
        sendTime: Instant,
        timeToLive: Duration
    ) {
        val value = ByteArray(1) { sendTime.epochSecond.toByte() }
        val key = getKey(userId).toByteArray()
        redisTemplate.opsForValue().setAndAwait(key, value, timeToLive)
    }
}