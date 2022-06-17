package com.munoon.heartbeatlive.server.ban.controller

import com.munoon.heartbeatlive.server.AbstractGraphqlHttpTest
import com.munoon.heartbeatlive.server.ban.UserBan
import com.munoon.heartbeatlive.server.ban.UserBanNotFoundByIdException
import com.munoon.heartbeatlive.server.ban.repository.UserBanRepository
import com.munoon.heartbeatlive.server.ban.service.UserBanService
import com.munoon.heartbeatlive.server.common.PageInfo
import com.munoon.heartbeatlive.server.subscription.Subscription
import com.munoon.heartbeatlive.server.subscription.SubscriptionNotFoundByIdException
import com.munoon.heartbeatlive.server.subscription.service.SubscriptionService
import com.munoon.heartbeatlive.server.utils.AuthTestUtils.withUser
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.expectSingleError
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.expectSingleUnauthenticatedError
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
import java.time.Instant
import java.time.OffsetDateTime

@SpringBootTest
internal class UserBanControllerTest : AbstractGraphqlHttpTest() {
    @SpykBean
    private lateinit var service: UserBanService

    @MockkBean
    private lateinit var subscriptionService: SubscriptionService

    @Autowired
    private lateinit var repository: UserBanRepository

    @Test
    fun banUserBySubscriptionId() {
        coEvery { subscriptionService.getSubscriptionById("subscription1") } returns Subscription(
            id = "subscription1",
            userId = "user1",
            subscriberUserId = "user2",
            receiveHeartRateMatchNotifications = false
        )

        val created = Instant.now()
        coEvery { service.banUser(userId = "user1", userIdToBan = "user2") } returns UserBan(
            id = "ban1",
            userId = "user1",
            bannedUserId = "user2",
            created = created
        )

        graphqlTester.withUser(id = "user1")
            .document("""
                mutation {
                    banUserBySubscriptionId(subscriptionId: "subscription1") {
                        id, banTime
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("banUserBySubscriptionId.id").isEqualsTo("ban1")
            .path("banUserBySubscriptionId.banTime").isEqualsTo(created.epochSecond)

        coVerify(exactly = 1) { subscriptionService.getSubscriptionById("subscription1") }
        coVerify(exactly = 1) { service.banUser(userId = "user1", userIdToBan = "user2") }
    }

    @Test
    fun `banUserBySubscriptionId - subscription not found`() {
        coEvery { subscriptionService.getSubscriptionById("subscription1") } throws
                SubscriptionNotFoundByIdException("subscriptionId")

        graphqlTester.withUser(id = "user1")
            .document("""
                mutation {
                    banUserBySubscriptionId(subscriptionId: "subscription1") {
                        id, banTime
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.NOT_FOUND,
                code = "subscription.not_found.by_id",
                extensions = mapOf("id" to "subscriptionId"),
                path = "banUserBySubscriptionId"
            )

        coVerify(exactly = 1) { subscriptionService.getSubscriptionById("subscription1") }
        coVerify(exactly = 0) { service.banUser(userId = any(), userIdToBan = any()) }
    }

    @Test
    fun `banUserBySubscriptionId - subscription belong to another user`() {
        coEvery { subscriptionService.getSubscriptionById("subscription1") } returns Subscription(
            id = "subscription1",
            userId = "user2",
            subscriberUserId = "user1",
            receiveHeartRateMatchNotifications = false
        )

        graphqlTester.withUser(id = "user1")
            .document("""
                mutation {
                    banUserBySubscriptionId(subscriptionId: "subscription1") {
                        id, banTime
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.NOT_FOUND,
                code = "subscription.not_found.by_id",
                extensions = mapOf("id" to "subscription1"),
                path = "banUserBySubscriptionId"
            )

        coVerify(exactly = 1) { subscriptionService.getSubscriptionById("subscription1") }
        coVerify(exactly = 0) { service.banUser(userId = any(), userIdToBan = any()) }
    }

    @Test
    fun `banUserBySubscriptionId - user not authenticated`() {
        graphqlTester
            .document("""
                mutation {
                    banUserBySubscriptionId(subscriptionId: "subscription1") {
                        id, banTime
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleUnauthenticatedError(path = "banUserBySubscriptionId")

        coVerify(exactly = 0) { subscriptionService.getSubscriptionById(any()) }
        coVerify(exactly = 0) { service.banUser(userId = any(), userIdToBan = any()) }
    }

    @Test
    fun unbanUserById() {
        coEvery { service.unbanUser(banId = "ban1", validateUserId = "user1") } returns Unit

        graphqlTester.withUser(id = "user1")
            .document("""mutation { unbanUserById(id: "ban1") }""")
            .execute()
            .satisfyNoErrors()
            .path("unbanUserById").isEqualsTo(true)

        coVerify(exactly = 1) { service.unbanUser(banId = "ban1", validateUserId = "user1") }
    }

    @Test
    fun `unbanUserById - ban is not found`() {
        coEvery { service.unbanUser(banId = "ban1", validateUserId = "user1") } throws
                UserBanNotFoundByIdException("ban1")

        graphqlTester.withUser(id = "user1")
            .document("""mutation { unbanUserById(id: "ban1") }""")
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.NOT_FOUND,
                code = "ban.not_found.by_id",
                extensions = mapOf("id" to "ban1"),
                path = "unbanUserById"
            )

        coVerify(exactly = 1) { service.unbanUser(banId = "ban1", validateUserId = "user1") }
    }

    @Test
    fun `unbanUserById - user not authenticated`() {
        graphqlTester
            .document("""mutation { unbanUserById(id: "ban1") }""")
            .execute()
            .errors().expectSingleUnauthenticatedError(path = "unbanUserById")

        coVerify(exactly = 0) { service.unbanUser(banId = any(), validateUserId = any()) }
    }

    @Test
    fun `getBannedUsers - sort by CREATED_ASC`() {
        val ban1 = runBlocking {
            val created = OffsetDateTime.now().minusDays(1).toInstant()
            repository.save(UserBan(userId = "user1", bannedUserId = "user2", created = created))
        }
        val ban2 = runBlocking {
            repository.save(UserBan(userId = "user1", bannedUserId = "user3", created = Instant.now()))
        }

        val expectedBan1 = mapOf("id" to ban1.id, "banTime" to ban1.created.epochSecond.toInt())
        val expectedBan2 = mapOf("id" to ban2.id, "banTime" to ban2.created.epochSecond.toInt())

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getBannedUsers(page: 0, size: 10, sort: CREATED_ASC) {
                        pageInfo { totalPages, totalItems, hasNext }
                        content { id, banTime }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getBannedUsers.pageInfo").isEqualsTo(PageInfo(totalPages = 1, totalItems = 2, hasNext = false))
            .path("getBannedUsers.content").isEqualsToListOf(expectedBan1, expectedBan2)

        val pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "created"))
        coVerify(exactly = 1) { service.getBannedUsers("user1", pageRequest) }
    }

    @Test
    fun `getBannedUsers - sort by CREATED_DESC`() {
        val ban1 = runBlocking {
            val created = OffsetDateTime.now().minusDays(1).toInstant()
            repository.save(UserBan(userId = "user1", bannedUserId = "user2", created = created))
        }
        val ban2 = runBlocking {
            repository.save(UserBan(userId = "user1", bannedUserId = "user3", created = Instant.now()))
        }

        val expectedBan1 = mapOf("id" to ban1.id, "banTime" to ban1.created.epochSecond.toInt())
        val expectedBan2 = mapOf("id" to ban2.id, "banTime" to ban2.created.epochSecond.toInt())

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getBannedUsers(page: 0, size: 10, sort: CREATED_DESC) {
                        pageInfo { totalPages, totalItems, hasNext }
                        content { id, banTime }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getBannedUsers.pageInfo").isEqualsTo(PageInfo(totalPages = 1, totalItems = 2, hasNext = false))
            .path("getBannedUsers.content").isEqualsToListOf(expectedBan2, expectedBan1)

        val pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "created"))
        coVerify(exactly = 1) { service.getBannedUsers("user1", pageRequest) }
    }

    @Test
    fun `getBannedUsers - user not authenticated`() {
        graphqlTester
            .document("""
                query {
                    getBannedUsers(page: 0, size: 10, sort: CREATED_DESC) {
                        pageInfo { totalPages, totalItems, hasNext }
                        content { id, banTime }
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleUnauthenticatedError(path = "getBannedUsers")

        coVerify(exactly = 0) { service.getBannedUsers(any(), any()) }
    }
}