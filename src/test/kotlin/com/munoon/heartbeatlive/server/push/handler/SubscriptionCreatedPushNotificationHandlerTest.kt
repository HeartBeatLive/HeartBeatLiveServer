package com.munoon.heartbeatlive.server.push.handler

import com.munoon.heartbeatlive.server.AbstractTest
import com.munoon.heartbeatlive.server.push.NewSubscriptionPushNotificationData
import com.munoon.heartbeatlive.server.push.service.PushNotificationService
import com.munoon.heartbeatlive.server.push.service.sendNotifications
import com.munoon.heartbeatlive.server.subscription.Subscription
import com.munoon.heartbeatlive.server.subscription.SubscriptionEvent
import com.munoon.heartbeatlive.server.user.model.GraphqlFirebaseCreateUserInput
import com.munoon.heartbeatlive.server.user.service.UserService
import com.ninjasquad.springmockk.MockkBean
import io.kotest.common.runBlocking
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.uuid
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.coVerify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher

@SpringBootTest
internal class SubscriptionCreatedPushNotificationHandlerTest : AbstractTest() {
    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Autowired
    private lateinit var userService: UserService

    @MockkBean
    private lateinit var pushNotificationService: PushNotificationService

    @Test
    fun handleNewSubscriptionEvent() {
        val subscriptionArbitrary = arbitrary { Subscription(
            id = Arb.uuid().map { it.toString() }.bind(),
            userId = Arb.uuid().map { it.toString() }.bind(),
            subscriberUserId = Arb.uuid().map { it.toString() }.bind(),
            receiveHeartRateMatchNotifications = Arb.boolean().bind()
        ) }

        coEvery { pushNotificationService.sendNotifications(any()) } returns Unit
        runBlocking { checkAll(10, subscriptionArbitrary, Arb.string().orNull()) {
                subscription, userDisplayName ->
            runBlocking {
                userService.createUser(
                    GraphqlFirebaseCreateUserInput(
                        id = subscription.subscriberUserId,
                        email = "email@gmail.com",
                        emailVerified = false
                    )
                )
                if (userDisplayName != null) {
                    userService.updateUserDisplayName(
                        userId = subscription.subscriberUserId,
                        displayName = userDisplayName
                    )
                }
            }

            val event = SubscriptionEvent.SubscriptionCreatedEvent(subscription)
            eventPublisher.publishEvent(event)

            val expectedData = NewSubscriptionPushNotificationData(
                subscriptionId = subscription.id!!,
                userId = subscription.userId,
                subscriberDisplayName = userDisplayName ?: "User"
            )
            coVerify(exactly = 1) { pushNotificationService.sendNotifications(expectedData) }
        } }
    }
}