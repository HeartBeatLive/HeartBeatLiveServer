package com.munoon.heartbeatlive.server.push.controller

import com.munoon.heartbeatlive.server.AbstractGraphqlHttpTest
import com.munoon.heartbeatlive.server.common.PageInfo
import com.munoon.heartbeatlive.server.common.PageResult
import com.munoon.heartbeatlive.server.push.PushNotification
import com.munoon.heartbeatlive.server.push.PushNotificationNotFoundByIdException
import com.munoon.heartbeatlive.server.push.service.PushNotificationService
import com.munoon.heartbeatlive.server.subscription.Subscription
import com.munoon.heartbeatlive.server.subscription.service.SubscriptionService
import com.munoon.heartbeatlive.server.user.UserMapper.asGraphqlPublicProfile
import com.munoon.heartbeatlive.server.user.UserTestUtils
import com.munoon.heartbeatlive.server.user.service.UserService
import com.munoon.heartbeatlive.server.utils.AuthTestUtils.withUser
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.expectSingleError
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.expectSingleUnauthenticatedError
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.isEqualsTo
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.satisfyNoErrors
import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.kotest.common.runBlocking
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.instant
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.graphql.execution.ErrorType

@SpringBootTest
internal class UserPushNotificationsControllerTest : AbstractGraphqlHttpTest() {
    @MockkBean
    private lateinit var service: PushNotificationService

    @SpykBean
    private lateinit var subscriptionService: SubscriptionService

    @SpykBean
    private lateinit var userService: UserService

    @Test
    fun getPushNotifications() {
        val firstPushNotification = PushNotification(
            userId = "user1",
            data = PushNotification.Data.NewSubscriberData(subscriptionId = "subscriptionId", "subscriberUserId")
        )
        val secondPushNotification = PushNotification(
            userId = "user1",
            data = PushNotification.Data.BanData(bannedByUserId = "user2")
        )

        coEvery { service.getPushNotificationsByUserId(any(), any()) } returns PageResult(
            data = listOf(firstPushNotification, secondPushNotification).asFlow(),
            totalItemsCount = 2
        )

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getPushNotifications(page: 0, size: 10, sort: CREATED_DESC) {
                        content {
                            id,
                            created,
                            data {
                                __typename
                            }
                        },
                        pageInfo {
                            totalPages,
                            totalItems,
                            hasNext
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getPushNotifications.pageInfo").isEqualsTo(PageInfo(totalItems = 2, totalPages = 1, hasNext = false))
            .path("getPushNotifications.content[0].id").isEqualsTo(firstPushNotification.id)
            .path("getPushNotifications.content[0].created").isEqualsTo(firstPushNotification.created.epochSecond)
            .path("getPushNotifications.content[0].data.__typename")
                .isEqualsTo("NewSubscriberPushNotificationData")
            .path("getPushNotifications.content[1].id").isEqualsTo(secondPushNotification.id)
            .path("getPushNotifications.content[1].created").isEqualsTo(secondPushNotification.created.epochSecond)
            .path("getPushNotifications.content[1].data.__typename")
                .isEqualsTo("BannedPushNotificationData")

        val expectedPageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "created"))
        coVerify(exactly = 1) { service.getPushNotificationsByUserId("user1", expectedPageRequest) }
    }

    @Test
    fun `getPushNotifications - not authenticated`() {
        graphqlTester
            .document("""
                query {
                    getPushNotifications(page: 0, size: 10, sort: CREATED_DESC) {
                        content {
                            id,
                            created,
                            data {
                                __typename
                            }
                        },
                        pageInfo {
                            totalPages,
                            totalItems,
                            hasNext
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleUnauthenticatedError(path = "getPushNotifications")

        coVerify(exactly = 0) { service.getPushNotificationsByUserId(any(), any()) }
    }

    @Test
    fun getPushNotificationById() {
        val pushNotification = PushNotification(
            userId = "user1",
            data = PushNotification.Data.NewSubscriberData(subscriptionId = "subscriptionId", "subscriberUserId")
        )

        coEvery { service.getPushNotificationById(any()) } returns pushNotification

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getPushNotificationById(id: "pushNotificationId") {
                        id,
                        created,
                        data {
                            __typename
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getPushNotificationById.id").isEqualsTo(pushNotification.id)
            .path("getPushNotificationById.created").isEqualsTo(pushNotification.created.epochSecond)
            .path("getPushNotificationById.data.__typename")
                .isEqualsTo("NewSubscriberPushNotificationData")

        coVerify(exactly = 1) { service.getPushNotificationById("pushNotificationId") }
    }

    @Test
    fun `getPushNotificationById - not authenticated`() {
        graphqlTester
            .document("""
                query {
                    getPushNotificationById(id: "pushNotificationId") {
                        id,
                        created,
                        data {
                            __typename
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleUnauthenticatedError(path = "getPushNotificationById")

        coVerify(exactly = 0) { service.getPushNotificationById(any()) }
    }

    @Test
    fun `getPushNotificationById - not found`() {
        coEvery { service.getPushNotificationById(any()) } throws
                PushNotificationNotFoundByIdException("pushNotificationId")

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getPushNotificationById(id: "pushNotificationId") {
                        id,
                        created,
                        data {
                            __typename
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.NOT_FOUND,
                code = "push_notification.not_found.by_id",
                extensions = mapOf("id" to "pushNotificationId"),
                path = "getPushNotificationById"
            )

        coVerify(exactly = 1) { service.getPushNotificationById("pushNotificationId") }
    }

    @Test
    fun `getPushNotificationById - another owner`() {
        val pushNotification = PushNotification(
            userId = "user2",
            data = PushNotification.Data.NewSubscriberData(subscriptionId = "subscriptionId", "subscriberUserId")
        )

        coEvery { service.getPushNotificationById(any()) } returns pushNotification

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getPushNotificationById(id: "pushNotificationId") {
                        id,
                        created,
                        data {
                            __typename
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.NOT_FOUND,
                code = "push_notification.not_found.by_id",
                extensions = mapOf("id" to "pushNotificationId"),
                path = "getPushNotificationById"
            )

        coVerify(exactly = 1) { service.getPushNotificationById("pushNotificationId") }
    }

    @Test
    fun getNewSubscriberPushNotificationDataSubscription(): Unit = runBlocking {
        val subscriptionArbitrary = arbitrary { Subscription(
            id = Arb.string(codepoints = Codepoint.alphanumeric(), size = 20).bind(),
            created = Arb.instant().bind(),
            userId = Arb.string(codepoints = Codepoint.alphanumeric(), size = 20).bind(),
            subscriberUserId = Arb.string(codepoints = Codepoint.alphanumeric(), size = 20).bind(),
            receiveHeartRateMatchNotifications = Arb.boolean().bind(),
            lock = Subscription.Lock(
                byPublisher = Arb.boolean().bind(),
                bySubscriber = Arb.boolean().bind()
            )
        ) }

        checkAll(5, subscriptionArbitrary) { subscription ->
            coEvery { subscriptionService.getAllByIds(any()) } returns listOf(subscription).asFlow()

            coEvery { service.getPushNotificationById(any()) } returns PushNotification(
                userId = "user1",
                data = PushNotification.Data.NewSubscriberData(subscriptionId = subscription.id!!, "subscriberUserId")
            )

            graphqlTester.withUser(id = "user1")
                .document("""
                query {
                    getPushNotificationById(id: "pushNotificationId") {
                        data {
                            ... on NewSubscriberPushNotificationData {
                                subscription {
                                    id,
                                    subscribeTime,
                                    lock {
                                        locked,
                                        byPublisher,
                                        bySubscriber
                                    }
                                }
                            }
                        }
                    }
                }
            """.trimIndent())
                .execute()
                .satisfyNoErrors()
                .path("getPushNotificationById.data.subscription.id").isEqualsTo(subscription.id!!)
                .path("getPushNotificationById.data.subscription.subscribeTime")
                    .isEqualsTo(subscription.created.epochSecond)
                .path("getPushNotificationById.data.subscription.lock.locked")
                    .isEqualsTo(subscription.lock.byPublisher || subscription.lock.bySubscriber)
                .path("getPushNotificationById.data.subscription.lock.byPublisher")
                    .isEqualsTo(subscription.lock.byPublisher)
                .path("getPushNotificationById.data.subscription.lock.bySubscriber")
                    .isEqualsTo(subscription.lock.bySubscriber)

            coVerify(exactly = 1) { subscriptionService.getAllByIds(setOf(subscription.id!!)) }
        }
    }

    @Test
    fun `getNewSubscriberPushNotificationDataSubscription - not found`() {
        coEvery { subscriptionService.getAllByIds(any()) } returns flowOf()

        coEvery { service.getPushNotificationById(any()) } returns PushNotification(
            userId = "user1",
            data = PushNotification.Data.NewSubscriberData(subscriptionId = "subscriptionId", "subscriberUserId")
        )

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getPushNotificationById(id: "pushNotificationId") {
                        data {
                            ... on NewSubscriberPushNotificationData {
                                subscription {
                                    id,
                                    subscribeTime,
                                    lock {
                                        locked,
                                        byPublisher,
                                        bySubscriber
                                    }
                                }
                            }
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getPushNotificationById.data.subscription").valueIsNull()

        coVerify(exactly = 1) { subscriptionService.getAllByIds(setOf("subscriptionId")) }
    }

    @Test
    fun getBannedPushNotificationDataBannedByUser(): Unit = runBlocking {
        checkAll(5, UserTestUtils.userArbitrary) { user ->
            coEvery { userService.getUsersByIds(any()) } returns flowOf(user)

            coEvery { service.getPushNotificationById(any()) } returns PushNotification(
                userId = "user1",
                data = PushNotification.Data.BanData(bannedByUserId = user.id)
            )

            graphqlTester.withUser(id = "user1")
                .document("""
                query {
                    getPushNotificationById(id: "pushNotificationId") {
                        data {
                            ... on BannedPushNotificationData {
                                bannedByUser {
                                    displayName
                                }
                            }
                        }
                    }
                }
            """.trimIndent())
                .execute()
                .satisfyNoErrors()
                .path("getPushNotificationById.data.bannedByUser").isEqualsTo(user.asGraphqlPublicProfile())

            coVerify(exactly = 1) { userService.getUsersByIds(setOf(user.id)) }
        }
    }

    @Test
    fun `getBannedPushNotificationDataBannedByUser - not found`() {
        coEvery { userService.getUsersByIds(any()) } returns flowOf()

        coEvery { service.getPushNotificationById(any()) } returns PushNotification(
            userId = "user1",
            data = PushNotification.Data.BanData(bannedByUserId = "userId")
        )

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getPushNotificationById(id: "pushNotificationId") {
                        data {
                            ... on BannedPushNotificationData {
                                bannedByUser {
                                    displayName
                                }
                            }
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getPushNotificationById.data.bannedByUser").valueIsNull()

        coVerify(exactly = 1) { userService.getUsersByIds(setOf("userId")) }
    }

    @Test
    fun getHighHeartRatePushNotificationDataHeartRateOwner(): Unit = runBlocking {
        checkAll(5, UserTestUtils.userArbitrary, Arb.int(range = 200..300)) { user, heartRate ->
            coEvery { userService.getUsersByIds(any()) } returns flowOf(user)

            coEvery { service.getPushNotificationById(any()) } returns PushNotification(
                userId = "user1",
                data = PushNotification.Data.HighHeartRateData(
                    heartRate = heartRate.toFloat(), heartRateOwnerUserId = user.id)
            )

            graphqlTester.withUser(id = "user1")
                .document("""
                query {
                    getPushNotificationById(id: "pushNotificationId") {
                        data {
                            ... on HighHeartRatePushNotificationData {
                                heartRate,
                                heartRateOwner {
                                    displayName
                                }
                            }
                        }
                    }
                }
            """.trimIndent())
                .execute()
                .satisfyNoErrors()
                .path("getPushNotificationById.data.heartRate").isEqualsTo(heartRate)
                .path("getPushNotificationById.data.heartRateOwner").isEqualsTo(user.asGraphqlPublicProfile())

            coVerify(exactly = 1) { userService.getUsersByIds(setOf(user.id)) }
        }
    }

    @Test
    fun `getHighHeartRatePushNotificationDataHeartRateOwner - not found`() {
        coEvery { userService.getUsersByIds(any()) } returns flowOf()

        coEvery { service.getPushNotificationById(any()) } returns PushNotification(
            userId = "user1",
            data = PushNotification.Data.HighHeartRateData(heartRate = 300f, heartRateOwnerUserId = "userId")
        )

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getPushNotificationById(id: "pushNotificationId") {
                        data {
                            ... on HighHeartRatePushNotificationData {
                                heartRate,
                                heartRateOwner {
                                    displayName
                                }
                            }
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getPushNotificationById.data.heartRate").isEqualsTo(300)
            .path("getPushNotificationById.data.heartRateOwner").valueIsNull()

        coVerify(exactly = 1) { userService.getUsersByIds(setOf("userId")) }
    }

    @Test
    fun getLowHeartRatePushNotificationDataHeartRateOwner(): Unit = runBlocking {
        checkAll(5, UserTestUtils.userArbitrary, Arb.int(range = 1..30)) { user, heartRate ->
            coEvery { userService.getUsersByIds(any()) } returns flowOf(user)

            coEvery { service.getPushNotificationById(any()) } returns PushNotification(
                userId = "user1",
                data = PushNotification.Data.LowHeartRateData(
                    heartRate = heartRate.toFloat(), heartRateOwnerUserId = user.id)
            )

            graphqlTester.withUser(id = "user1")
                .document("""
                query {
                    getPushNotificationById(id: "pushNotificationId") {
                        data {
                            ... on LowHeartRatePushNotificationData {
                                heartRate,
                                heartRateOwner {
                                    displayName
                                }
                            }
                        }
                    }
                }
            """.trimIndent())
                .execute()
                .satisfyNoErrors()
                .path("getPushNotificationById.data.heartRate").isEqualsTo(heartRate)
                .path("getPushNotificationById.data.heartRateOwner").isEqualsTo(user.asGraphqlPublicProfile())

            coVerify(exactly = 1) { userService.getUsersByIds(setOf(user.id)) }
        }
    }

    @Test
    fun `getLowHeartRatePushNotificationDataHeartRateOwner - not found`() {
        coEvery { userService.getUsersByIds(any()) } returns flowOf()

        coEvery { service.getPushNotificationById(any()) } returns PushNotification(
            userId = "user1",
            data = PushNotification.Data.LowHeartRateData(heartRate = 1f, heartRateOwnerUserId = "userId")
        )

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getPushNotificationById(id: "pushNotificationId") {
                        data {
                            ... on LowHeartRatePushNotificationData {
                                heartRate,
                                heartRateOwner {
                                    displayName
                                }
                            }
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getPushNotificationById.data.heartRate").isEqualsTo(1)
            .path("getPushNotificationById.data.heartRateOwner").valueIsNull()

        coVerify(exactly = 1) { userService.getUsersByIds(setOf("userId")) }
    }

    @Test
    fun getHeartRateMatchPushNotificationDataMatchWithUser(): Unit = runBlocking {
        checkAll(5, UserTestUtils.userArbitrary, Arb.int(range = 50..150)) { user, heartRate ->
            coEvery { userService.getUsersByIds(any()) } returns flowOf(user)

            coEvery { service.getPushNotificationById(any()) } returns PushNotification(
                userId = "user1",
                data = PushNotification.Data.HeartRateMatchData(
                    heartRate = heartRate.toFloat(), matchWithUserId = user.id)
            )

            graphqlTester.withUser(id = "user1")
                .document("""
                query {
                    getPushNotificationById(id: "pushNotificationId") {
                        data {
                            ... on HeartRateMatchPushNotificationData {
                                heartRate,
                                matchWithUser {
                                    displayName
                                }
                            }
                        }
                    }
                }
            """.trimIndent())
                .execute()
                .satisfyNoErrors()
                .path("getPushNotificationById.data.heartRate").isEqualsTo(heartRate)
                .path("getPushNotificationById.data.matchWithUser").isEqualsTo(user.asGraphqlPublicProfile())

            coVerify(exactly = 1) { userService.getUsersByIds(setOf(user.id)) }
        }
    }

    @Test
    fun `getHeartRateMatchPushNotificationDataMatchWithUser - not found`() {
        coEvery { userService.getUsersByIds(any()) } returns flowOf()

        coEvery { service.getPushNotificationById(any()) } returns PushNotification(
            userId = "user1",
            data = PushNotification.Data.HeartRateMatchData(heartRate = 60f, matchWithUserId = "userId")
        )

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getPushNotificationById(id: "pushNotificationId") {
                        data {
                            ... on HeartRateMatchPushNotificationData {
                                heartRate,
                                matchWithUser {
                                    displayName
                                }
                            }
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getPushNotificationById.data.heartRate").isEqualsTo(60)
            .path("getPushNotificationById.data.matchWithUser").valueIsNull()

        coVerify(exactly = 1) { userService.getUsersByIds(setOf("userId")) }
    }
}