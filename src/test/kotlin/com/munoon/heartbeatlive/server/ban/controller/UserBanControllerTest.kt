package com.munoon.heartbeatlive.server.ban.controller

import com.munoon.heartbeatlive.server.AbstractGraphqlHttpTest
import com.munoon.heartbeatlive.server.ban.UserBan
import com.munoon.heartbeatlive.server.ban.repository.UserBanRepository
import com.munoon.heartbeatlive.server.ban.service.UserBanService
import com.munoon.heartbeatlive.server.common.PageInfo
import com.munoon.heartbeatlive.server.subscription.Subscription
import com.munoon.heartbeatlive.server.subscription.service.SubscriptionService
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
            subscriberUserId = "user2"
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
    @Disabled("Test will be implemented when error schema will be specified")
    fun `banUserBySubscriptionId - subscription not found`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `banUserBySubscriptionId - subscription belong to another user`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `banUserBySubscriptionId - user not authenticated`() {
        // TODO impl this when error schema will be ready
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
    @Disabled("Test will be implemented when error schema will be specified")
    fun `unbanUserById - ban is not found`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `unbanUserById - ban belong to other user`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `unbanUserById - user not authenticated`() {
        // TODO impl this when error schema will be ready
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
    @Disabled("Test will be implemented when error schema will be specified")
    fun `getBannedUsers - user not authenticated`() {
        // TODO impl this when error schema will be ready
    }
}