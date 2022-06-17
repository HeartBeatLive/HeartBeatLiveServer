package com.munoon.heartbeatlive.server.heartrate.handler

import com.munoon.heartbeatlive.server.user.service.UserService
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class UserStatusUpdaterHeartRateInfoHandler(
    private val userService: UserService,
) : HeartRateInfoHandler {
    override suspend fun handleHeartRateInfo(userId: String, heartRate: Float) {
        userService.writeUserHeartRate(userId, heartRate, Instant.now())
    }
}