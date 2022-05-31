package com.munoon.heartbeatlive.server.heartrate.publisher

import com.munoon.heartbeatlive.server.messaging.HeartRateMessageOuterClass.HeartRateMessage

interface HeartRateMessagePublisher {
    suspend fun publish(message: HeartRateMessage)
}