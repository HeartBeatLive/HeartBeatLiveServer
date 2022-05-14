package com.munoon.heartbeatlive.server.subscription.controller

import com.munoon.heartbeatlive.server.AbstractGraphqlHttpTest
import com.munoon.heartbeatlive.server.common.PageInfo
import com.munoon.heartbeatlive.server.subscription.Subscription
import com.munoon.heartbeatlive.server.subscription.repository.SubscriptionRepository
import com.munoon.heartbeatlive.server.subscription.service.SubscriptionService
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.service.UserService
import com.munoon.heartbeatlive.server.utils.AuthTestUtils.withUser
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.isEqualsTo
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.isEqualsToListOf
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.satisfyNoErrors
import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime

@SpringBootTest
internal class SubscriptionControllerTest : AbstractGraphqlHttpTest() {
    @SpykBean
    private lateinit var service: SubscriptionService

    @MockkBean
    private lateinit var userService: UserService

    @Autowired
    private lateinit var repository: SubscriptionRepository

    @Test
    fun subscribeBySharingCode() {
        val created = Instant.now()
        coEvery { service.subscribeBySharingCode("ABC123", "user1") } returns Subscription(
            id = "subscription1",
            userId = "user2",
            subscriberUserId = "user1",
            created = created
        )

        graphqlTester.withUser(id = "user1")
            .document("""
                mutation {
                    subscribeBySharingCode(sharingCode: "ABC123") {
                        id, subscribeTime
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("subscribeBySharingCode.id").isEqualsTo("subscription1")
            .path("subscribeBySharingCode.subscribeTime").isEqualsTo(Instant.ofEpochSecond(created.epochSecond))

        coVerify(exactly = 1) { service.subscribeBySharingCode(any(), any()) }
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `subscribeBySharingCode - self subscription`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `subscribeBySharingCode - sharing code expired`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `subscribeBySharingCode - sharing code invalid`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `subscribeBySharingCode - user have too many subscribers`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `subscribeBySharingCode - user have too many subscriptions`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `subscribeBySharingCode - user not authenticated`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    fun unsubscribeFromUserById() {
        coEvery { service.unsubscribeFromUserById("subscription1", validateUserId = "user1") } returns Unit

        graphqlTester.withUser(id = "user1")
            .document("""
                mutation {
                    unsubscribeFromUserById(id: "subscription1")
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("unsubscribeFromUserById").isEqualsTo(true)

        coVerify(exactly = 1) { service.unsubscribeFromUserById(any(), any()) }
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `unsubscribeFromUserById - subscription not found`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `unsubscribeFromUserById - subscription belong to another user`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `unsubscribeFromUserById - user not authenticated`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    fun `getSubscriptionById - by subscriber`() {
        val created = Instant.now()
        coEvery { service.getSubscriptionById("subscriptionId") } returns Subscription(
            id = "subscriptionId",
            userId = "user1",
            subscriberUserId = "user2",
            created = created
        )

        graphqlTester.withUser(id = "user2")
            .document("""
                query {
                    getSubscriptionById(id: "subscriptionId") {
                        id, subscribeTime
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getSubscriptionById.id").isEqualsTo("subscriptionId")
            .path("getSubscriptionById.subscribeTime").isEqualsTo(Instant.ofEpochSecond(created.epochSecond))

        coVerify(exactly = 1) { service.getSubscriptionById(any()) }
    }

    @Test
    fun `getSubscriptionById - by user`() {
        val created = Instant.now()
        coEvery { service.getSubscriptionById("subscriptionId") } returns Subscription(
            id = "subscriptionId",
            userId = "user1",
            subscriberUserId = "user2",
            created = created
        )

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getSubscriptionById(id: "subscriptionId") {
                        id, subscribeTime
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getSubscriptionById.id").isEqualsTo("subscriptionId")
            .path("getSubscriptionById.subscribeTime").isEqualsTo(Instant.ofEpochSecond(created.epochSecond))

        coVerify(exactly = 1) { service.getSubscriptionById(any()) }
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `getSubscriptionById - not found`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    fun `getSubscriptionById - belong to another users`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    fun `getSubscriptionById - user not authenticated`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    fun `getProfileSubscribers - sort by CREATED_ASC`() {
        coEvery { userService.getUserById("user1") } returns User(
            id = "user1",
            displayName = null,
            email = null,
            emailVerified = false
        )

        val expectedPageInfo = PageInfo(totalPages = 1, totalItems = 2, hasNext = false)

        val created1 = OffsetDateTime.now().minus(Duration.ofDays(1)).toInstant()
        val created2 = Instant.now()

        val subscription1 = runBlocking {
            repository.save(Subscription(userId = "user1", subscriberUserId = "user2", created = created1))
        }
        val subscription2 = runBlocking {
            repository.save(Subscription(userId = "user1", subscriberUserId = "user3", created = created2))
        }

        val expectedItem1 = mapOf("id" to subscription1.id, "subscribeTime" to created1.epochSecond.toInt())
        val expectedItem2 = mapOf("id" to subscription2.id, "subscribeTime" to created2.epochSecond.toInt())

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getProfile {
                        subscribers(page: 0, size: 10, sort: CREATED_ASC) {
                            content { id, subscribeTime }
                            pageInfo { totalPages, totalItems, hasNext }
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getProfile.subscribers.content").isEqualsToListOf(expectedItem1, expectedItem2)
            .path("getProfile.subscribers.pageInfo").isEqualsTo(expectedPageInfo)

        val expectedPageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "created"))
        coVerify(exactly = 1) { service.getSubscribers("user1", expectedPageRequest) }
    }

    @Test
    fun `getProfileSubscribers - sort by CREATED_DESC`() {
        coEvery { userService.getUserById("user1") } returns User(
            id = "user1",
            displayName = null,
            email = null,
            emailVerified = false
        )

        val expectedPageInfo = PageInfo(totalPages = 1, totalItems = 2, hasNext = false)

        val created1 = OffsetDateTime.now().minus(Duration.ofDays(1)).toInstant()
        val created2 = Instant.now()

        val subscription1 = runBlocking {
            repository.save(Subscription(userId = "user1", subscriberUserId = "user2", created = created1))
        }
        val subscription2 = runBlocking {
            repository.save(Subscription(userId = "user1", subscriberUserId = "user3", created = created2))
        }

        val expectedItem1 = mapOf("id" to subscription1.id, "subscribeTime" to created1.epochSecond.toInt())
        val expectedItem2 = mapOf("id" to subscription2.id, "subscribeTime" to created2.epochSecond.toInt())

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getProfile {
                        subscribers(page: 0, size: 10, sort: CREATED_DESC) {
                            content { id, subscribeTime }
                            pageInfo { totalPages, totalItems, hasNext }
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getProfile.subscribers.content").isEqualsToListOf(expectedItem2, expectedItem1)
            .path("getProfile.subscribers.pageInfo").isEqualsTo(expectedPageInfo)

        val expectedPageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "created"))
        coVerify(exactly = 1) { service.getSubscribers("user1", expectedPageRequest) }
    }

    @Test
    fun `getProfileSubscriptions - sort by CREATED_ASC`() {
        coEvery { userService.getUserById("user1") } returns User(
            id = "user1",
            displayName = null,
            email = null,
            emailVerified = false
        )

        val expectedPageInfo = PageInfo(totalPages = 1, totalItems = 2, hasNext = false)

        val created1 = OffsetDateTime.now().minus(Duration.ofDays(1)).toInstant()
        val created2 = Instant.now()

        val subscription1 = runBlocking {
            repository.save(Subscription(userId = "user2", subscriberUserId = "user1", created = created1))
        }
        val subscription2 = runBlocking {
            repository.save(Subscription(userId = "user3", subscriberUserId = "user1", created = created2))
        }

        val expectedItem1 = mapOf("id" to subscription1.id, "subscribeTime" to created1.epochSecond.toInt())
        val expectedItem2 = mapOf("id" to subscription2.id, "subscribeTime" to created2.epochSecond.toInt())

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getProfile {
                        subscriptions(page: 0, size: 10, sort: CREATED_ASC) {
                            content { id, subscribeTime }
                            pageInfo { totalPages, totalItems, hasNext }
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getProfile.subscriptions.content").isEqualsToListOf(expectedItem1, expectedItem2)
            .path("getProfile.subscriptions.pageInfo").isEqualsTo(expectedPageInfo)

        val expectedPageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "created"))
        coVerify(exactly = 1) { service.getSubscriptions("user1", expectedPageRequest) }
    }

    @Test
    fun `getProfileSubscriptions - sort by CREATED_DESC`() {
        coEvery { userService.getUserById("user1") } returns User(
            id = "user1",
            displayName = null,
            email = null,
            emailVerified = false
        )

        val expectedPageInfo = PageInfo(totalPages = 1, totalItems = 2, hasNext = false)

        val created1 = OffsetDateTime.now().minus(Duration.ofDays(1)).toInstant()
        val created2 = Instant.now()

        val subscription1 = runBlocking {
            repository.save(Subscription(userId = "user2", subscriberUserId = "user1", created = created1))
        }
        val subscription2 = runBlocking {
            repository.save(Subscription(userId = "user3", subscriberUserId = "user1", created = created2))
        }

        val expectedItem1 = mapOf("id" to subscription1.id, "subscribeTime" to created1.epochSecond.toInt())
        val expectedItem2 = mapOf("id" to subscription2.id, "subscribeTime" to created2.epochSecond.toInt())

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getProfile {
                        subscriptions(page: 0, size: 10, sort: CREATED_DESC) {
                            content { id, subscribeTime }
                            pageInfo { totalPages, totalItems, hasNext }
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getProfile.subscriptions.content").isEqualsToListOf(expectedItem2, expectedItem1)
            .path("getProfile.subscriptions.pageInfo").isEqualsTo(expectedPageInfo)

        val expectedPageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "created"))
        coVerify(exactly = 1) { service.getSubscriptions("user1", expectedPageRequest) }
    }
}