package com.munoon.heartbeatlive.server.config

import com.google.protobuf.Message
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.RedisSerializer

@Configuration
class RedisConfig {
    @Bean
    fun protobufRedisTemplate(
        connectionFactory: ReactiveRedisConnectionFactory,
    ): ReactiveRedisTemplate<Message, Message> {
        val serializer = object : AbstractProtobufRedisSerializer<Message>() {
            override fun deserialize(bytes: ByteArray?): Message? {
                throw UnsupportedOperationException("This serializer can't deserialize bytes.")
            }
        }
        val serializationContext = RedisSerializationContext.fromSerializer(serializer)
        return ReactiveRedisTemplate(connectionFactory, serializationContext)
    }
}

abstract class AbstractProtobufRedisSerializer<T : Message> : RedisSerializer<T> {
    override fun serialize(t: T?): ByteArray? {
        return t?.toByteArray() ?: return null
    }
}