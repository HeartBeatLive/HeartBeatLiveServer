package com.munoon.heartbeatlive.server.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.time.Duration

@Component
@ConfigurationProperties("app.heart-rate-stream")
class HeartRateStreamProperties {
    lateinit var heartRateTimeToSend: Duration
    var subscriptionsCountLimitPerUser: Int = 15
    lateinit var leaveUserOnlineSinceLastHeartRateDuration: Duration
}