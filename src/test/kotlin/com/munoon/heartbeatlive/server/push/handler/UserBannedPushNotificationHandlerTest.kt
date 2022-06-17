package com.munoon.heartbeatlive.server.push.handler

import com.munoon.heartbeatlive.server.AbstractTest
import com.munoon.heartbeatlive.server.ban.UserBanEvents
import com.munoon.heartbeatlive.server.push.BanPushNotificationData
import com.munoon.heartbeatlive.server.push.service.PushNotificationService
import com.munoon.heartbeatlive.server.push.service.sendNotifications
import com.munoon.heartbeatlive.server.user.model.GraphqlFirebaseCreateUserInput
import com.munoon.heartbeatlive.server.user.service.UserService
import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.kotest.common.runBlocking
import io.mockk.coEvery
import io.mockk.coVerify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher

@SpringBootTest
internal class UserBannedPushNotificationHandlerTest : AbstractTest() {
    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    @SpykBean
    private lateinit var userService: UserService

    @MockkBean
    private lateinit var pushNotificationService: PushNotificationService

    @Test
    fun handlerUserBannedEvent() {
        val expectedData = BanPushNotificationData(
            userId = "user2",
            bannedByUserId = "user1",
            bannedByUserDisplayName = "Nikita"
        )
        coEvery { pushNotificationService.sendNotifications(any()) } returns Unit

        runBlocking {
            userService.createUser(GraphqlFirebaseCreateUserInput(id = "user1", email = null, emailVerified = false))
            userService.updateUserDisplayName(userId = "user1", displayName = "Nikita")
        }

        eventPublisher.publishEvent(UserBanEvents.UserBannedEvent("user2", "user1"))

        coVerify(exactly = 1) { pushNotificationService.sendNotifications(expectedData) }
        coVerify { userService.getUserById("user1") }
    }
}