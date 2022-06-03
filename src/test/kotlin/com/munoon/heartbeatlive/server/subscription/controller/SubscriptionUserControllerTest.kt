package com.munoon.heartbeatlive.server.subscription.controller

import com.munoon.heartbeatlive.server.AbstractGraphqlHttpTest
import com.munoon.heartbeatlive.server.subscription.Subscription
import com.munoon.heartbeatlive.server.subscription.service.SubscriptionService
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.model.GraphqlPublicProfileTo
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
internal class SubscriptionUserControllerTest : AbstractGraphqlHttpTest() {
    @MockkBean
    private lateinit var userService: UserService

    @MockkBean
    private lateinit var service: SubscriptionService

    @Test
    fun getSubscriptionUser() {
        coEvery { userService.getUsersByIds(setOf("user1")) } returns flowOf(User(
            id = "user1",
            displayName = "Test User",
            email = null,
            emailVerified = false
        ))

        coEvery { service.getSubscriptionById("subscriptionId") } returns Subscription(
            id = "subscriptionId",
            userId = "user1",
            subscriberUserId = "user2",
            created = Instant.now()
        )

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getSubscriptionById(id: "subscriptionId") {
                        user { displayName }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getSubscriptionById.user.displayName").isEqualsTo("Test User")

        coVerify(exactly = 1) { userService.getUsersByIds(setOf("user1")) }
    }

    @Test
    fun getSubscriptionSubscriber() {
        coEvery { userService.getUsersByIds(setOf("user2")) } returns flowOf(User(
            id = "user2",
            displayName = "Subscriber User",
            email = null,
            emailVerified = false
        ))

        coEvery { service.getSubscriptionById("subscriptionId") } returns Subscription(
            id = "subscriptionId",
            userId = "user1",
            subscriberUserId = "user2",
            created = Instant.now()
        )

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getSubscriptionById(id: "subscriptionId") {
                        subscriber { displayName }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getSubscriptionById.subscriber").isEqualsTo(GraphqlPublicProfileTo("Subscriber User"))

        coVerify(exactly = 1) { userService.getUsersByIds(setOf("user2")) }
    }
}