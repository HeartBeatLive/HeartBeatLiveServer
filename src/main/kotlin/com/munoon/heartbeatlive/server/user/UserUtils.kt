package com.munoon.heartbeatlive.server.user

import com.munoon.heartbeatlive.server.config.properties.HeartRateStreamProperties
import com.munoon.heartbeatlive.server.user.model.GraphqlUserHeartRateOnlineStatus
import java.time.Instant

object UserUtils {
    fun getHeartRateOnlineStatus(
        lastHeartRateInfoReceiveTime: Instant?,
        properties: HeartRateStreamProperties
    ): GraphqlUserHeartRateOnlineStatus {
        val lastReceiveTime = (lastHeartRateInfoReceiveTime ?: return GraphqlUserHeartRateOnlineStatus.OFFLINE)
        return if (lastReceiveTime + properties.leaveUserOnlineSinceLastHeartRateDuration > Instant.now())
            GraphqlUserHeartRateOnlineStatus.ONLINE else GraphqlUserHeartRateOnlineStatus.OFFLINE
    }
}