package com.munoon.heartbeatlive.server.push.handler

import com.munoon.heartbeatlive.server.push.NewSubscriptionPushNotificationData
import com.munoon.heartbeatlive.server.push.service.PushNotificationService
import com.munoon.heartbeatlive.server.subscription.SubscriptionEvent
import com.munoon.heartbeatlive.server.user.service.UserService
import kotlinx.coroutines.runBlocking
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Controller

@Controller
class SubscriptionCreatedPushNotificationHandler(
    private val pushNotificationService: PushNotificationService,
    private val userService: UserService
) {
    @Async
    @EventListener
    fun handleNewSubscriptionEvent(event: SubscriptionEvent.SubscriptionCreatedEvent) = runBlocking {
        val subscriberDisplayName = userService.getUserById(event.subscription.subscriberUserId).displayName
        pushNotificationService.sendNotification(NewSubscriptionPushNotificationData(
            subscriptionId = event.subscription.id!!,
            userId = event.subscription.userId,
            subscriberDisplayName = subscriberDisplayName ?: "User"
        ))
    }
}