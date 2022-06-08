package com.munoon.heartbeatlive.server.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.stereotype.Component
import java.time.Duration

@Component
@ConfigurationProperties("app.heart-rate-stream")
class HeartRateStreamProperties {
    lateinit var heartRateTimeToSend: Duration
    var subscriptionsCountLimitPerUser: Int = 15
    lateinit var leaveUserOnlineSinceLastHeartRateDuration: Duration
    lateinit var highLowPush: HighLowPushSettings

    @ConstructorBinding
    data class HighLowPushSettings(
        val normalHeartRate: NormalHeartRateSettings,
        val sendPushTimeoutDuration: Duration
    ) {
        @ConstructorBinding
        data class NormalHeartRateSettings(
            val min: Float,
            val max: Float
        )
    }
}