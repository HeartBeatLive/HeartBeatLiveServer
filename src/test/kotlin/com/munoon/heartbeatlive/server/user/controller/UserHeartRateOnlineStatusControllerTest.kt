package com.munoon.heartbeatlive.server.user.controller

import com.munoon.heartbeatlive.server.AbstractGraphqlHttpTest
import com.munoon.heartbeatlive.server.subscription.Subscription
import com.munoon.heartbeatlive.server.subscription.service.SubscriptionService
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.service.UserService
import com.munoon.heartbeatlive.server.utils.AuthTestUtils.withUser
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.isEqualsTo
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.satisfyNoErrors
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant

@SpringBootTest
internal class UserHeartRateOnlineStatusControllerTest : AbstractGraphqlHttpTest() {
    @MockkBean
    private lateinit var userService: UserService

    @MockkBean
    private lateinit var subscriptionService: SubscriptionService

    @Test
    fun getProfileHeartRateOnlineStatus() {
        coEvery { userService.getUserById("1") } returns User(
            id = "user1",
            displayName = null,
            email = null,
            emailVerified = false,
            lastHeartRateInfoReceiveTime = Instant.now()
        )

        graphqlTester.withUser()
            .document("""
                query {
                    getProfile {
                        heartRateOnlineStatus
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getProfile.heartRateOnlineStatus").isEqualsTo("ONLINE")
    }

    @Test
    fun getSubscriptionUserProfileHeartRateOnlineStatus() {
        coEvery { userService.getUsersByIds(setOf("user1")) } returns flowOf(User(
            id = "user1",
            displayName = "Test User",
            email = null,
            emailVerified = false,
            lastHeartRateInfoReceiveTime = Instant.now()
        ))

        coEvery { subscriptionService.getSubscriptionById("subscriptionId") } returns Subscription(
            id = "subscriptionId",
            userId = "user1",
            subscriberUserId = "user2",
            created = Instant.now()
        )

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getSubscriptionById(id: "subscriptionId") {
                        user { heartRateOnlineStatus }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getSubscriptionById.user.heartRateOnlineStatus").isEqualsTo("ONLINE")

        coVerify(exactly = 1) { userService.getUsersByIds(setOf("user1")) }
    }
}