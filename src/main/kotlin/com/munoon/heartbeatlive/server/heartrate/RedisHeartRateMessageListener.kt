package com.munoon.heartbeatlive.server.heartrate

import com.munoon.heartbeatlive.server.config.AbstractProtobufRedisSerializer
import com.munoon.heartbeatlive.server.config.ServerInstanceRunningId
import com.munoon.heartbeatlive.server.config.properties.HeartRateStreamProperties
import com.munoon.heartbeatlive.server.messaging.HeartRateMessageOuterClass
import kotlinx.coroutines.reactor.mono
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.stereotype.Component
import reactor.core.scheduler.Schedulers
import java.time.Instant

@Component
class RedisHeartRateMessageListener(
    private val redisConnectionFactory: ReactiveRedisConnectionFactory,
    private val heartRateStreamProperties: HeartRateStreamProperties,
    private val heartBeatSubscribersManager: HeartBeatSubscribersManager
) {
    companion object {
        const val HEART_RATE_CHANNEL = "heartBeat.heartRate"
    }

    @EventListener(ContextRefreshedEvent::class)
    fun subscribeToHeartRateMessages() {
        val serializer = object : AbstractProtobufRedisSerializer<HeartRateMessageOuterClass.HeartRateMessage>() {
            override fun deserialize(bytes: ByteArray?): HeartRateMessageOuterClass.HeartRateMessage? {
                return HeartRateMessageOuterClass.HeartRateMessage.parseFrom(bytes ?: return null)
            }
        }

        ReactiveRedisTemplate(redisConnectionFactory, RedisSerializationContext.fromSerializer(serializer))
            .listenToChannel(HEART_RATE_CHANNEL)
            .filter { it.message.publisherId != ServerInstanceRunningId.id }
            .filter {
                val publishTime = Instant.ofEpochMilli(it.message.publishTimeEpochMillis)
                publishTime.plus(heartRateStreamProperties.heartRateTimeToSend) > Instant.now()
            }
            .flatMap { mono {
                val message = it.message
                heartBeatSubscribersManager.sendHeartRate(message.userId, message.heartRate)
            } }
            .subscribeOn(Schedulers.newBoundedElastic(
                Schedulers.DEFAULT_BOUNDED_ELASTIC_SIZE,
                Schedulers.DEFAULT_BOUNDED_ELASTIC_QUEUESIZE,
                "heartRateMessagesListener",
                60,
                true
            ))
            .subscribe()
    }
}