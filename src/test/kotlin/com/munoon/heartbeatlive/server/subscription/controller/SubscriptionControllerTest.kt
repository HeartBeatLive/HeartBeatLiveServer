package com.munoon.heartbeatlive.server.subscription.controller

import com.munoon.heartbeatlive.server.AbstractGraphqlHttpTest
import com.munoon.heartbeatlive.server.common.PageInfo
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharingExpiredException
import com.munoon.heartbeatlive.server.subscription.SelfSubscriptionAttemptException
import com.munoon.heartbeatlive.server.subscription.Subscription
import com.munoon.heartbeatlive.server.subscription.SubscriptionNotFoundByIdException
import com.munoon.heartbeatlive.server.subscription.UserSubscribersLimitExceededException
import com.munoon.heartbeatlive.server.subscription.UserSubscriptionsLimitExceededException
import com.munoon.heartbeatlive.server.subscription.account.JwtUserSubscription
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import com.munoon.heartbeatlive.server.subscription.model.GraphqlSubscribeOptionsInput
import com.munoon.heartbeatlive.server.subscription.repository.SubscriptionRepository
import com.munoon.heartbeatlive.server.subscription.service.SubscriptionService
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.service.UserService
import com.munoon.heartbeatlive.server.utils.AuthTestUtils.withUser
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.expectSingleError
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.expectSingleUnauthenticatedError
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.expectSingleValidationError
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.isEqualsTo
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.isEqualsToListOf
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.satisfyNoErrors
import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.graphql.execution.ErrorType
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
        val expectedOptions = GraphqlSubscribeOptionsInput(receiveHeartRateMatchNotifications = true)
        coEvery { service.subscribeBySharingCode("ABC123", "user1", any(), any()) } returns Subscription(
            id = "subscription1",
            userId = "user2",
            subscriberUserId = "user1",
            created = created,
            receiveHeartRateMatchNotifications = false,
            lock = Subscription.Lock(bySubscriber = true)
        )

        val subscription = JwtUserSubscription(UserSubscriptionPlan.PRO, Instant.now().plusSeconds(60))
        graphqlTester.withUser(id = "user1", subscription = subscription)
            .document("""
                mutation {
                    subscribeBySharingCode(
                        sharingCode: "ABC123",
                        options: { receiveHeartRateMatchNotifications: true }
                    ) {
                        id,
                        subscribeTime,
                        lock {
                            locked,
                            byPublisher,
                            bySubscriber
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("subscribeBySharingCode.id").isEqualsTo("subscription1")
            .path("subscribeBySharingCode.subscribeTime").isEqualsTo(Instant.ofEpochSecond(created.epochSecond))
            .path("subscribeBySharingCode.lock.locked").isEqualsTo(true)
            .path("subscribeBySharingCode.lock.byPublisher").isEqualsTo(false)
            .path("subscribeBySharingCode.lock.bySubscriber").isEqualsTo(true)

        coVerify(exactly = 1) {
            service.subscribeBySharingCode("ABC123", "user1", UserSubscriptionPlan.PRO, expectedOptions)
        }
    }

    @Test
    fun `subscribeBySharingCode - self subscription`() {
        coEvery { service.subscribeBySharingCode("ABC123", "user1", any(), any()) } throws
                SelfSubscriptionAttemptException()

        graphqlTester.withUser(id = "user1")
            .document("""
                mutation {
                    subscribeBySharingCode(sharingCode: "ABC123") {
                        id,
                        subscribeTime,
                        lock {
                            locked,
                            byPublisher,
                            bySubscriber
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.BAD_REQUEST,
                code = "subscription.self_subscribe",
                path = "subscribeBySharingCode"
            )

        coVerify(exactly = 1) { service.subscribeBySharingCode(any(), any(), UserSubscriptionPlan.FREE, any()) }
    }

    @Test
    fun `subscribeBySharingCode - sharing code expired`() {
        coEvery { service.subscribeBySharingCode("ABC123", "user1", any(), any()) } throws
                HeartBeatSharingExpiredException()

        graphqlTester.withUser(id = "user1")
            .document("""
                mutation {
                    subscribeBySharingCode(sharingCode: "ABC123") {
                        id,
                        subscribeTime,
                        lock {
                            locked,
                            byPublisher,
                            bySubscriber
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.FORBIDDEN,
                code = "heart_beat_sharing.expired",
                path = "subscribeBySharingCode"
            )

        coVerify(exactly = 1) { service.subscribeBySharingCode(any(), any(), any(), any()) }
    }

    @Test
    fun `subscribeBySharingCode - sharing code invalid`() {
        graphqlTester.withUser(id = "user1")
            .document("""
                mutation {
                    subscribeBySharingCode(sharingCode: "") {
                        id,
                        subscribeTime,
                        lock {
                            locked,
                            byPublisher,
                            bySubscriber
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleValidationError("subscribeBySharingCode", "sharingCode")

        coVerify(exactly = 0) { service.subscribeBySharingCode(any(), any(), any(), any()) }
    }

    @Test
    fun `subscribeBySharingCode - user have too many subscribers`() {
        coEvery { service.subscribeBySharingCode("ABC123", "user1", any(), any()) } throws
                UserSubscribersLimitExceededException()

        graphqlTester.withUser(id = "user1")
            .document("""
                mutation {
                    subscribeBySharingCode(sharingCode: "ABC123") {
                        id,
                        subscribeTime,
                        lock {
                            locked,
                            byPublisher,
                            bySubscriber
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.FORBIDDEN,
                code = "subscription.subscribers_limit_exceeded",
                path = "subscribeBySharingCode"
            )

        coVerify(exactly = 1) { service.subscribeBySharingCode(any(), any(), any(), any()) }
    }

    @Test
    fun `subscribeBySharingCode - user have too many subscriptions`() {
        coEvery { service.subscribeBySharingCode("ABC123", "user1", any(), any()) } throws
                UserSubscriptionsLimitExceededException()

        graphqlTester.withUser(id = "user1")
            .document("""
                mutation {
                    subscribeBySharingCode(sharingCode: "ABC123") {
                        id,
                        subscribeTime,
                        lock {
                            locked,
                            byPublisher,
                            bySubscriber
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.FORBIDDEN,
                code = "subscription.subscriptions_limit_exceeded",
                path = "subscribeBySharingCode"
            )

        coVerify(exactly = 1) { service.subscribeBySharingCode(any(), any(), any(), any()) }
    }

    @Test
    fun `subscribeBySharingCode - user not authenticated`() {
        graphqlTester
            .document("""
                mutation {
                    subscribeBySharingCode(sharingCode: "ABC123") {
                        id,
                        subscribeTime,
                        lock {
                            locked,
                            byPublisher,
                            bySubscriber
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleUnauthenticatedError(path = "subscribeBySharingCode")

        coVerify(exactly = 0) { service.subscribeBySharingCode(any(), any(), any(), any()) }
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
    fun `unsubscribeFromUserById - subscription not found`() {
        coEvery { service.unsubscribeFromUserById("subscription1", validateUserId = "user1") } throws
                SubscriptionNotFoundByIdException("subscription1")

        graphqlTester.withUser(id = "user1")
            .document("""
                mutation {
                    unsubscribeFromUserById(id: "subscription1")
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.NOT_FOUND,
                code = "subscription.not_found.by_id",
                extensions = mapOf("id" to "subscription1"),
                path = "unsubscribeFromUserById"
            )

        coVerify(exactly = 1) { service.unsubscribeFromUserById(any(), any()) }
    }

    @Test
    fun `unsubscribeFromUserById - user not authenticated`() {
        graphqlTester
            .document("""
                mutation {
                    unsubscribeFromUserById(id: "subscription1")
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleUnauthenticatedError(path = "unsubscribeFromUserById")

        coVerify(exactly = 0) { service.unsubscribeFromUserById(any(), any()) }
    }

    @Test
    fun `getSubscriptionById - by subscriber`() {
        val created = Instant.now()
        coEvery { service.getSubscriptionById("subscriptionId") } returns Subscription(
            id = "subscriptionId",
            userId = "user1",
            subscriberUserId = "user2",
            created = created,
            receiveHeartRateMatchNotifications = false,
            lock = Subscription.Lock(bySubscriber = true)
        )

        graphqlTester.withUser(id = "user2")
            .document("""
                query {
                    getSubscriptionById(id: "subscriptionId") {
                        id,
                        subscribeTime,
                        lock {
                            locked,
                            byPublisher,
                            bySubscriber
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getSubscriptionById.id").isEqualsTo("subscriptionId")
            .path("getSubscriptionById.subscribeTime").isEqualsTo(Instant.ofEpochSecond(created.epochSecond))
            .path("getSubscriptionById.lock.locked").isEqualsTo(true)
            .path("getSubscriptionById.lock.byPublisher").isEqualsTo(false)
            .path("getSubscriptionById.lock.bySubscriber").isEqualsTo(true)

        coVerify(exactly = 1) { service.getSubscriptionById(any()) }
    }

    @Test
    fun `getSubscriptionById - by user`() {
        val created = Instant.now()
        coEvery { service.getSubscriptionById("subscriptionId") } returns Subscription(
            id = "subscriptionId",
            userId = "user1",
            subscriberUserId = "user2",
            created = created,
            receiveHeartRateMatchNotifications = false,
            lock = Subscription.Lock(bySubscriber = true)
        )

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getSubscriptionById(id: "subscriptionId") {
                        id,
                        subscribeTime,
                        lock {
                            locked,
                            byPublisher,
                            bySubscriber
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getSubscriptionById.id").isEqualsTo("subscriptionId")
            .path("getSubscriptionById.subscribeTime").isEqualsTo(Instant.ofEpochSecond(created.epochSecond))
            .path("getSubscriptionById.lock.locked").isEqualsTo(true)
            .path("getSubscriptionById.lock.byPublisher").isEqualsTo(false)
            .path("getSubscriptionById.lock.bySubscriber").isEqualsTo(true)

        coVerify(exactly = 1) { service.getSubscriptionById(any()) }
    }

    @Test
    fun `getSubscriptionById - not found`() {
        coEvery { service.getSubscriptionById("subscriptionId") } throws
                SubscriptionNotFoundByIdException("subscriptionId")

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getSubscriptionById(id: "subscriptionId") {
                        id,
                        subscribeTime,
                        lock {
                            locked,
                            byPublisher,
                            bySubscriber
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.NOT_FOUND,
                code = "subscription.not_found.by_id",
                extensions = mapOf("id" to "subscriptionId"),
                path = "getSubscriptionById"
            )

        coVerify(exactly = 1) { service.getSubscriptionById(any()) }
    }

    @Test
    fun `getSubscriptionById - belong to another users`() {
        coEvery { service.getSubscriptionById("subscriptionId") } returns Subscription(
            id = "subscriptionId",
            userId = "user2",
            subscriberUserId = "user3",
            created = Instant.now(),
            receiveHeartRateMatchNotifications = false
        )

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getSubscriptionById(id: "subscriptionId") {
                        id,
                        subscribeTime,
                        lock {
                            locked,
                            byPublisher,
                            bySubscriber
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.NOT_FOUND,
                code = "subscription.not_found.by_id",
                extensions = mapOf("id" to "subscriptionId"),
                path = "getSubscriptionById"
            )

        coVerify(exactly = 1) { service.getSubscriptionById(any()) }
    }

    @Test
    fun `getSubscriptionById - user not authenticated`() {
        graphqlTester
            .document("""
                query {
                    getSubscriptionById(id: "subscriptionId") {
                        id,
                        subscribeTime,
                        lock {
                            locked,
                            byPublisher,
                            bySubscriber
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleUnauthenticatedError(path = "getSubscriptionById")

        coVerify(exactly = 0) { service.getSubscriptionById(any()) }
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
            repository.save(Subscription(userId = "user1", subscriberUserId = "user2", created = created1,
                receiveHeartRateMatchNotifications = false, lock = Subscription.Lock(bySubscriber = true)))
        }
        val subscription2 = runBlocking {
            repository.save(Subscription(userId = "user1", subscriberUserId = "user3", created = created2,
                receiveHeartRateMatchNotifications = false))
        }

        val expectedItem1 = mapOf("id" to subscription1.id, "subscribeTime" to created1.epochSecond.toInt(),
            "lock" to mapOf("locked" to true, "byPublisher" to false, "bySubscriber" to true))
        val expectedItem2 = mapOf("id" to subscription2.id, "subscribeTime" to created2.epochSecond.toInt(),
            "lock" to mapOf("locked" to false, "byPublisher" to false, "bySubscriber" to false))

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getProfile {
                        subscribers(page: 0, size: 10, sort: CREATED_ASC) {
                            content {
                                id,
                                subscribeTime,
                                lock {
                                    locked,
                                    byPublisher,
                                    bySubscriber
                                }
                            }
                            pageInfo {
                                totalPages,
                                totalItems,
                                hasNext
                            }
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
            repository.save(Subscription(userId = "user1", subscriberUserId = "user2", created = created1,
                receiveHeartRateMatchNotifications = false, lock = Subscription.Lock(bySubscriber = true)))
        }
        val subscription2 = runBlocking {
            repository.save(Subscription(userId = "user1", subscriberUserId = "user3", created = created2,
                receiveHeartRateMatchNotifications = false))
        }

        val expectedItem1 = mapOf("id" to subscription1.id, "subscribeTime" to created1.epochSecond.toInt(),
            "lock" to mapOf("locked" to true, "byPublisher" to false, "bySubscriber" to true))
        val expectedItem2 = mapOf("id" to subscription2.id, "subscribeTime" to created2.epochSecond.toInt(),
            "lock" to mapOf("locked" to false, "byPublisher" to false, "bySubscriber" to false))

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getProfile {
                        subscribers(page: 0, size: 10, sort: CREATED_DESC) {
                            content {
                                id,
                                subscribeTime,
                                lock {
                                    locked,
                                    byPublisher,
                                    bySubscriber
                                }
                            }
                            pageInfo {
                                totalPages,
                                totalItems,
                                hasNext
                            }
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
            repository.save(Subscription(userId = "user2", subscriberUserId = "user1", created = created1,
                receiveHeartRateMatchNotifications = false, lock = Subscription.Lock(bySubscriber = true)))
        }
        val subscription2 = runBlocking {
            repository.save(Subscription(userId = "user3", subscriberUserId = "user1", created = created2,
                receiveHeartRateMatchNotifications = false))
        }

        val expectedItem1 = mapOf("id" to subscription1.id, "subscribeTime" to created1.epochSecond.toInt(),
            "lock" to mapOf("locked" to true, "byPublisher" to false, "bySubscriber" to true))
        val expectedItem2 = mapOf("id" to subscription2.id, "subscribeTime" to created2.epochSecond.toInt(),
            "lock" to mapOf("locked" to false, "byPublisher" to false, "bySubscriber" to false))

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getProfile {
                        subscriptions(page: 0, size: 10, sort: CREATED_ASC) {
                            content {
                                id,
                                subscribeTime,
                                lock {
                                    locked,
                                    byPublisher,
                                    bySubscriber
                                }
                            }
                                pageInfo {
                                totalPages,
                                totalItems,
                                hasNext
                            }
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
            repository.save(Subscription(userId = "user2", subscriberUserId = "user1", created = created1,
                receiveHeartRateMatchNotifications = false, lock = Subscription.Lock(bySubscriber = true)))
        }
        val subscription2 = runBlocking {
            repository.save(Subscription(userId = "user3", subscriberUserId = "user1", created = created2,
                receiveHeartRateMatchNotifications = false))
        }

        val expectedItem1 = mapOf("id" to subscription1.id, "subscribeTime" to created1.epochSecond.toInt(),
            "lock" to mapOf("locked" to true, "byPublisher" to false, "bySubscriber" to true))
        val expectedItem2 = mapOf("id" to subscription2.id, "subscribeTime" to created2.epochSecond.toInt(),
            "lock" to mapOf("locked" to false, "byPublisher" to false, "bySubscriber" to false))

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getProfile {
                        subscriptions(page: 0, size: 10, sort: CREATED_DESC) {
                            content {
                                id,
                                subscribeTime,
                                lock {
                                    locked,
                                    byPublisher,
                                    bySubscriber
                                }
                            }
                            pageInfo {
                                totalPages,
                                totalItems,
                                hasNext
                            }
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