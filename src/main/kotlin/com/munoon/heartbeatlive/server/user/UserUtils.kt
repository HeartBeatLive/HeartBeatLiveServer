package com.munoon.heartbeatlive.server.user

import com.munoon.heartbeatlive.server.config.properties.HeartRateStreamProperties
import com.munoon.heartbeatlive.server.user.model.GraphqlUserHeartRateOnlineStatus
import java.time.Instant

object UserUtils {
    fun getHeartRateOnlineStatus(
        heartRates: List<User.HeartRate>,
        properties: HeartRateStreamProperties
    ): GraphqlUserHeartRateOnlineStatus {
        val userOnline = heartRates.filter { it.time + properties.storeUserHeartRateDuration > Instant.now() }
            .maxByOrNull { it.time }
            ?.let { it.heartRate != null }
            ?: false

        return if (userOnline) GraphqlUserHeartRateOnlineStatus.ONLINE else GraphqlUserHeartRateOnlineStatus.OFFLINE
    }
}