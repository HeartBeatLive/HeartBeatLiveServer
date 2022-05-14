package com.munoon.heartbeatlive.server.sharing.controller

import com.munoon.heartbeatlive.server.AbstractGraphqlHttpTest
import com.munoon.heartbeatlive.server.common.PageInfo
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharing
import com.munoon.heartbeatlive.server.sharing.model.GraphqlCreateSharingCodeInput
import com.munoon.heartbeatlive.server.sharing.service.HeartBeatSharingService
import com.munoon.heartbeatlive.server.subscription.account.JwtUserSubscription
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.UserRole
import com.munoon.heartbeatlive.server.user.model.GraphqlProfileTo
import com.munoon.heartbeatlive.server.user.service.UserService
import com.munoon.heartbeatlive.server.utils.AuthTestUtils.withUser
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.isEqualsTo
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.isEqualsToListOf
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.satisfyNoErrors
import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime

@SpringBootTest
internal class HeartBeatSharingProfileControllerTest : AbstractGraphqlHttpTest() {
    @SpykBean
    private lateinit var service: HeartBeatSharingService

    @MockkBean
    private lateinit var userService: UserService

    @Test
    fun getSharingCodeById() {
        val created = Instant.now()
        val expiredAt = OffsetDateTime.now().plusDays(10).toInstant()
        coEvery { service.getSharingCodeById("sharingCode1") } returns HeartBeatSharing(
            id = "sharingCode1",
            publicCode = "ABC123",
            userId = "user1",
            created,
            expiredAt
        )

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getSharingCodeById(id: "sharingCode1") {
                        id, publicCode, sharingUrl, created, expiredAt
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getSharingCodeById.id").isEqualsTo("sharingCode1")
            .path("getSharingCodeById.publicCode").isEqualsTo("ABC123")
            .path("getSharingCodeById.sharingUrl").isEqualsTo("https://heartbeatlive.com/sharing/ABC123")
            .path("getSharingCodeById.created").isEqualsTo(created.epochSecond)
            .path("getSharingCodeById.expiredAt").isEqualsTo(expiredAt.epochSecond)

        coVerify(exactly = 1) { service.getSharingCodeById("sharingCode1") }
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `getSharingCodeById - not found`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `getSharingCodeById - other user`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `getSharingCodeById - not authenticated`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    fun createSharingCode() {
        val created = Instant.now()
        val expiredAt = OffsetDateTime.now().plusDays(10).toInstant()
        coEvery { service.createSharing(any(), any(), any()) } returns HeartBeatSharing(
            id = "sharingCode1",
            publicCode = "ABC123",
            userId = "user1",
            created,
            expiredAt
        )

        val subscription = JwtUserSubscription(UserSubscriptionPlan.PRO, Instant.now().plus(Duration.ofDays(1)))
        graphqlTester.withUser(id = "user1", subscription = subscription)
            .document("""
                mutation {
                    createSharingCode(data: { expiredAt: ${expiredAt.epochSecond} }) {
                        id, publicCode, sharingUrl, created, expiredAt
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("createSharingCode.id").isEqualsTo("sharingCode1")
            .path("createSharingCode.publicCode").isEqualsTo("ABC123")
            .path("createSharingCode.sharingUrl").isEqualsTo("https://heartbeatlive.com/sharing/ABC123")
            .path("createSharingCode.created").isEqualsTo(created.epochSecond)
            .path("createSharingCode.expiredAt").isEqualsTo(expiredAt.epochSecond)

        val expectedInput = GraphqlCreateSharingCodeInput(expiredAt = Instant.ofEpochSecond(expiredAt.epochSecond))
        coVerify(exactly = 1) { service.createSharing(expectedInput, "user1", UserSubscriptionPlan.PRO) }
    }

    @Test
    fun `createSharingCode - subscription expired`() {
        val created = Instant.now()
        val expiredAt = OffsetDateTime.now().plusDays(10).toInstant()
        coEvery { service.createSharing(any(), any(), any()) } returns HeartBeatSharing(
            id = "sharingCode1",
            publicCode = "ABC123",
            userId = "user1",
            created,
            expiredAt
        )

        val subscription = JwtUserSubscription(UserSubscriptionPlan.PRO, Instant.now().minus(Duration.ofDays(1)))
        graphqlTester.withUser(id = "user1", subscription = subscription)
            .document("""
                mutation {
                    createSharingCode(data: { expiredAt: ${expiredAt.epochSecond} }) {
                        id, publicCode, sharingUrl, created, expiredAt
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("createSharingCode.id").isEqualsTo("sharingCode1")
            .path("createSharingCode.publicCode").isEqualsTo("ABC123")
            .path("createSharingCode.sharingUrl").isEqualsTo("https://heartbeatlive.com/sharing/ABC123")
            .path("createSharingCode.created").isEqualsTo(created.epochSecond)
            .path("createSharingCode.expiredAt").isEqualsTo(expiredAt.epochSecond)

        val expectedInput = GraphqlCreateSharingCodeInput(expiredAt = Instant.ofEpochSecond(expiredAt.epochSecond))
        coVerify(exactly = 1) { service.createSharing(expectedInput, "user1", UserSubscriptionPlan.FREE) }
    }

    @Test
    fun `createSharingCode - empty data`() {
        val created = Instant.now()
        val expiredAt = OffsetDateTime.now().plusDays(10).toInstant()
        coEvery { service.createSharing(any(), any(), any()) } returns HeartBeatSharing(
            id = "sharingCode1",
            publicCode = "ABC123",
            userId = "user1",
            created,
            expiredAt
        )

        val subscription = JwtUserSubscription(UserSubscriptionPlan.PRO, Instant.now().plus(Duration.ofDays(1)))
        graphqlTester.withUser(id = "user1", subscription = subscription)
            .document("""
                mutation {
                    createSharingCode {
                        id, publicCode, sharingUrl, created, expiredAt
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("createSharingCode.id").isEqualsTo("sharingCode1")
            .path("createSharingCode.publicCode").isEqualsTo("ABC123")
            .path("createSharingCode.sharingUrl").isEqualsTo("https://heartbeatlive.com/sharing/ABC123")
            .path("createSharingCode.created").isEqualsTo(created.epochSecond)
            .path("createSharingCode.expiredAt").isEqualsTo(expiredAt.epochSecond)

        val expectedInput = GraphqlCreateSharingCodeInput(expiredAt = null)
        coVerify(exactly = 1) { service.createSharing(expectedInput, "user1", UserSubscriptionPlan.PRO) }
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `createSharingCode - limit exceeded`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `createSharingCode - invalid expired at`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `createSharingCode - unauthenticated`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    fun updateSharingCodeExpireTime() {
        val created = Instant.now()
        val expiredAt = OffsetDateTime.now().plusDays(10).withNano(0).toInstant()
        coEvery { service.updateSharingCodeExpireTime(any(), any(), any()) } returns HeartBeatSharing(
            id = "sharingCode1",
            publicCode = "ABC123",
            userId = "user1",
            created,
            expiredAt
        )

        graphqlTester.withUser(id = "user1")
            .document("""
                mutation {
                    updateSharingCodeExpireTime(id: "sharingCode1", expiredAt: ${expiredAt.epochSecond}) {
                        id, publicCode, sharingUrl, created, expiredAt
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("updateSharingCodeExpireTime.id").isEqualsTo("sharingCode1")
            .path("updateSharingCodeExpireTime.publicCode").isEqualsTo("ABC123")
            .path("updateSharingCodeExpireTime.sharingUrl").isEqualsTo("https://heartbeatlive.com/sharing/ABC123")
            .path("updateSharingCodeExpireTime.created").isEqualsTo(created.epochSecond)
            .path("updateSharingCodeExpireTime.expiredAt").isEqualsTo(expiredAt.epochSecond)

        coVerify(exactly = 1) {
            service.updateSharingCodeExpireTime("sharingCode1", expiredAt, "user1")
        }
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `updateSharingCodeExpireTime - not found`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `updateSharingCodeExpireTime - other user`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `updateSharingCodeExpireTime - invalid expired at`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `updateSharingCodeExpireTime - unauthenticated`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    fun deleteSharingCodeById() {
        coEvery { service.deleteSharingCodeById("sharingCode1", "user1") } returns Unit

        graphqlTester.withUser(id = "user1")
            .document("""mutation { deleteSharingCodeById(id: "sharingCode1") }""")
            .execute()
            .satisfyNoErrors()
            .path("deleteSharingCodeById").isEqualsTo(true)

        coVerify(exactly = 1) { service.deleteSharingCodeById("sharingCode1", "user1") }
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `deleteSharingCodeById - not found`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `deleteSharingCodeById - other user`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `deleteSharingCodeById - unauthenticated`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    fun `getProfileSharingCode - sort CREATED_DESC`() {
        coEvery { userService.getUserById(any()) } returns User(
            id = "userId",
            displayName = null,
            email = null,
            emailVerified = false
        )

        val expectedPageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "created"))
        val expectedPageInfo = PageInfo(totalPages = 1, totalItems = 3, hasNext = false)

        val input = GraphqlCreateSharingCodeInput(expiredAt = null)
        val sharing1 = runBlocking { service.createSharing(input, "user1", UserSubscriptionPlan.PRO) }
        val sharing2 = runBlocking { service.createSharing(input, "user1", UserSubscriptionPlan.PRO) }
        val sharing3 = runBlocking { service.createSharing(input, "user1", UserSubscriptionPlan.PRO) }

        val expectedContent = arrayOf(
            mapOf(
                "id" to sharing3.id,
                "publicCode" to sharing3.publicCode,
                "sharingUrl" to "https://heartbeatlive.com/sharing/${sharing3.publicCode}",
                "created" to sharing3.created.epochSecond.toInt(),
                "expiredAt" to null
            ),
            mapOf(
                "id" to sharing2.id,
                "publicCode" to sharing2.publicCode,
                "sharingUrl" to "https://heartbeatlive.com/sharing/${sharing2.publicCode}",
                "created" to sharing2.created.epochSecond.toInt(),
                "expiredAt" to null
            ),
            mapOf(
                "id" to sharing1.id,
                "publicCode" to sharing1.publicCode,
                "sharingUrl" to "https://heartbeatlive.com/sharing/${sharing1.publicCode}",
                "created" to sharing1.created.epochSecond.toInt(),
                "expiredAt" to null
            )
        )

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getProfile {
                        sharingCodes(page: 0, size: 10, sort: CREATED_DESC) {
                            content { id, publicCode, sharingUrl, created, expiredAt }
                            pageInfo { totalPages, totalItems, hasNext }
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getProfile.sharingCodes.pageInfo").isEqualsTo(expectedPageInfo)
            .path("getProfile.sharingCodes.content").isEqualsToListOf(*expectedContent)

        coVerify(exactly = 1) { service.getSharingCodesByUserId(expectedPageRequest, "user1") }
    }

    @Test
    fun `getProfileSharingCode - sort CREATED_ASC`() {
        coEvery { userService.getUserById(any()) } returns User(
            id = "userId",
            displayName = null,
            email = null,
            emailVerified = false
        )

        val expectedPageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "created"))
        val expectedPageInfo = PageInfo(totalPages = 1, totalItems = 3, hasNext = false)

        val input = GraphqlCreateSharingCodeInput(expiredAt = null)
        val sharing1 = runBlocking { service.createSharing(input, "user1", UserSubscriptionPlan.PRO) }
        val sharing2 = runBlocking { service.createSharing(input, "user1", UserSubscriptionPlan.PRO) }
        val sharing3 = runBlocking { service.createSharing(input, "user1", UserSubscriptionPlan.PRO) }

        val expectedContent = arrayOf(
            mapOf(
                "id" to sharing1.id,
                "publicCode" to sharing1.publicCode,
                "sharingUrl" to "https://heartbeatlive.com/sharing/${sharing1.publicCode}",
                "created" to sharing1.created.epochSecond.toInt(),
                "expiredAt" to null
            ),
            mapOf(
                "id" to sharing2.id,
                "publicCode" to sharing2.publicCode,
                "sharingUrl" to "https://heartbeatlive.com/sharing/${sharing2.publicCode}",
                "created" to sharing2.created.epochSecond.toInt(),
                "expiredAt" to null
            ),
            mapOf(
                "id" to sharing3.id,
                "publicCode" to sharing3.publicCode,
                "sharingUrl" to "https://heartbeatlive.com/sharing/${sharing3.publicCode}",
                "created" to sharing3.created.epochSecond.toInt(),
                "expiredAt" to null
            )
        )

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getProfile {
                        sharingCodes(page: 0, size: 10, sort: CREATED_ASC) {
                            content { id, publicCode, sharingUrl, created, expiredAt }
                            pageInfo { totalPages, totalItems, hasNext }
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getProfile.sharingCodes.pageInfo").isEqualsTo(expectedPageInfo)
            .path("getProfile.sharingCodes.content").isEqualsToListOf(*expectedContent)

        coVerify(exactly = 1) { service.getSharingCodesByUserId(expectedPageRequest, "user1") }
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `getProfileSharingCode - validation error`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `getProfileSharingCode - unauthenticated`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    fun mapSharingCodeUser() {
        coEvery { service.getSharingCodeById("sharingCode1") } returns HeartBeatSharing(
            id = "sharingCode1",
            publicCode = "ABC123",
            userId = "user1",
            expiredAt = null
        )

        coEvery { userService.getUsersByIds(any()) } returns flowOf(User(
            id = "user1",
            displayName = "Test User",
            email = "email@example.com",
            emailVerified = true,
            roles = setOf(UserRole.ADMIN)
        ))

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getSharingCodeById(id: "sharingCode1") {
                        id,
                        user {
                            id,
                            displayName,
                            email,
                            emailVerified,
                            roles
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getSharingCodeById.id").isEqualsTo("sharingCode1")
            .path("getSharingCodeById.user").isEqualsTo(GraphqlProfileTo(
                id = "user1",
                displayName = "Test User",
                email = "email@example.com",
                emailVerified = true,
                roles = setOf(UserRole.ADMIN)
            ))

        coVerify(exactly = 1) { service.getSharingCodeById("sharingCode1") }
        coVerify(exactly = 1) { userService.getUsersByIds(setOf("user1")) }
    }
}