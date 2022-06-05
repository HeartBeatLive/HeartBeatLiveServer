package com.munoon.heartbeatlive.server.push.handler

import com.munoon.heartbeatlive.server.ban.UserBanEvents
import com.munoon.heartbeatlive.server.push.BanPushNotificationData
import com.munoon.heartbeatlive.server.push.service.PushNotificationService
import com.munoon.heartbeatlive.server.user.service.UserService
import kotlinx.coroutines.runBlocking
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class UserBannedPushNotificationHandler(
    private val pushNotificationService: PushNotificationService,
    private val userService: UserService
) {
    @Async
    @EventListener
    fun handlerUserBannedEvent(event: UserBanEvents.UserBannedEvent) = runBlocking {
        val data = BanPushNotificationData(
            userId = event.userId,
            bannedByUserId = event.bannedByUserId,
            bannedByUserDisplayName = userService.getUserById(event.bannedByUserId).displayName ?: "User"
        )
        pushNotificationService.sendNotification(data)
    }
}