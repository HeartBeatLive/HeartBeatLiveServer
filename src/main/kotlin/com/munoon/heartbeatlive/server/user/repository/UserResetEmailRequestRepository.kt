package com.munoon.heartbeatlive.server.user.repository

import com.munoon.heartbeatlive.server.config.properties.UserProperties
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.getAndAwait
import org.springframework.data.redis.core.setAndAwait
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class UserResetEmailRequestRepository(
    connectionFactory: ReactiveRedisConnectionFactory,
    private val userProperties: UserProperties
) {
    private val redisTemplate = ReactiveRedisTemplate(connectionFactory, RedisSerializationContext.byteArray())
    private companion object {
        const val COLLECTION_NAME = "userResetEmailRequests"

        fun getKey(ipAddress: String) = "$COLLECTION_NAME:$ipAddress"
    }

    suspend fun checkIfIpAddressMadeARequest(ipAddress: String): Boolean {
        val key = getKey(ipAddress).toByteArray()
        return redisTemplate.opsForValue().getAndAwait(key) != null
    }

    suspend fun saveANewRequestForIpAddress(ipAddress: String, createTime: Instant) {
        val value = ByteArray(1) { createTime.epochSecond.toByte() }
        val key = getKey(ipAddress).toByteArray()
        redisTemplate.opsForValue().setAndAwait(key, value, userProperties.resetPasswordRequestTimeout)
    }
}