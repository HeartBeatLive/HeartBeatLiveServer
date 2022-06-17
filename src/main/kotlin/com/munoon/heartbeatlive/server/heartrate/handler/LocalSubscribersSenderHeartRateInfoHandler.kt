package com.munoon.heartbeatlive.server.heartrate.handler

import com.munoon.heartbeatlive.server.heartrate.HeartBeatSubscribersManager
import org.springframework.stereotype.Component

@Component
class LocalSubscribersSenderHeartRateInfoHandler(
    private val subscribersManager: HeartBeatSubscribersManager,
) : HeartRateInfoHandler {
    override suspend fun handleHeartRateInfo(userId: String, heartRate: Float) {
        subscribersManager.sendHeartRate(userId, heartRate)
    }
}