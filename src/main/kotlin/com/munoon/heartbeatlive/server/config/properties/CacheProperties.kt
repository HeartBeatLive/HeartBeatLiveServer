package com.munoon.heartbeatlive.server.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.stereotype.Component
import java.time.Duration

@Component
@ConfigurationProperties("cache")
class CacheProperties {
    lateinit var userSubscribers: UserSubscribersCacheSettings

    @ConstructorBinding
    data class UserSubscribersCacheSettings(
        val entryCapacity: Long,
        val idleScanTime: Duration
    )
}