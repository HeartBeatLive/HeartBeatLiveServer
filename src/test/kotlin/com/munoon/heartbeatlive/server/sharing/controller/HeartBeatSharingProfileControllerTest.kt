package com.munoon.heartbeatlive.server.sharing.controller

import com.munoon.heartbeatlive.server.AbstractGraphqlHttpTest
import com.munoon.heartbeatlive.server.common.PageInfo
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharing
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharingLimitExceededException
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharingNotFoundByIdException
import com.munoon.heartbeatlive.server.sharing.model.GraphqlCreateSharingCodeInput
import com.munoon.heartbeatlive.server.sharing.service.HeartBeatSharingService
import com.munoon.heartbeatlive.server.subscription.account.JwtUserSubscription
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.service.UserService
import com.munoon.heartbeatlive.server.utils.AuthTestUtils.withUser
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.expectSingleError
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.isEqualsTo
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.isEqualsToListOf
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.satisfyNoErrors
import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.graphql.execution.ErrorType
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
    fun `getSharingCodeById - not found`() {
        coEvery { service.getSharingCodeById("sharingCode1") } throws
                HeartBeatSharingNotFoundByIdException("sharingCode1")

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getSharingCodeById(id: "sharingCode1") {
                        id, publicCode, sharingUrl, created, expiredAt
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.NOT_FOUND,
                code = "heart_beat_sharing.not_found.by_id",
                extensions = mapOf("id" to "sharingCode1"),
                path = "getSharingCodeById"
            )

        coVerify(exactly = 1) { service.getSharingCodeById("sharingCode1") }
    }

    @Test
    fun `getSharingCodeById - other user`() {
        coEvery { service.getSharingCodeById("sharingCode1") } returns HeartBeatSharing(
            id = "sharingCode1",
            publicCode = "ABC123",
            userId = "user2",
            expiredAt = null
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
            .errors().expectSingleError(
                errorType = ErrorType.NOT_FOUND,
                code = "heart_beat_sharing.not_found.by_id",
                extensions = mapOf("id" to "sharingCode1"),
                path = "getSharingCodeById"
            )

        coVerify(exactly = 1) { service.getSharingCodeById("sharingCode1") }
    }

    @Test
    fun `getSharingCodeById - not authenticated`() {
        graphqlTester
            .document("""
                query {
                    getSharingCodeById(id: "sharingCode1") {
                        id, publicCode, sharingUrl, created, expiredAt
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.FORBIDDEN,
                code = "common.access_denied",
                path = "getSharingCodeById"
            )

        coVerify(exactly = 0) { service.getSharingCodeById("sharingCode1") }
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
    fun `createSharingCode - limit exceeded`() {
        coEvery { service.createSharing(any(), any(), any()) } throws HeartBeatSharingLimitExceededException(1)

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
            .errors().expectSingleError(
                errorType = ErrorType.FORBIDDEN,
                code = "heart_beat_sharing.limit_exceeded",
                extensions = mapOf("limit" to 1),
                path = "createSharingCode"
            )

        val expectedInput = GraphqlCreateSharingCodeInput(expiredAt = null)
        coVerify(exactly = 1) { service.createSharing(expectedInput, "user1", UserSubscriptionPlan.PRO) }
    }

    @Test
    fun `createSharingCode - invalid expired at`() {
        val expiredAt = OffsetDateTime.now().minusDays(100).toInstant()

        graphqlTester.withUser(id = "user1")
            .document("""
                mutation {
                    createSharingCode(data: { expiredAt: ${expiredAt.epochSecond} }) {
                        id, publicCode, sharingUrl, created, expiredAt
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.BAD_REQUEST,
                code = "common.validation",
                path = "createSharingCode",
                extensions = mapOf("invalidProperties" to listOf("data.expiredAt" ))
            )

        coVerify(exactly = 0) { service.createSharing(any(), any(), any()) }
    }

    @Test
    fun `createSharingCode - unauthenticated`() {
        graphqlTester
            .document("""
                mutation {
                    createSharingCode {
                        id, publicCode, sharingUrl, created, expiredAt
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.FORBIDDEN,
                code = "common.access_denied",
                path = "createSharingCode"
            )

        coVerify(exactly = 0) { service.createSharing(any(), any(), any()) }
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
    fun `updateSharingCodeExpireTime - not found`() {
        coEvery { service.updateSharingCodeExpireTime(any(), any(), any()) } throws
                HeartBeatSharingNotFoundByIdException("sharingCode1")

        val expiredAt = Instant.now().plusSeconds(100)
        graphqlTester.withUser(id = "user1")
            .document("""
                mutation {
                    updateSharingCodeExpireTime(id: "sharingCode1", expiredAt: ${expiredAt.epochSecond}) {
                        id, publicCode, sharingUrl, created, expiredAt
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.NOT_FOUND,
                code = "heart_beat_sharing.not_found.by_id",
                extensions = mapOf("id" to "sharingCode1"),
                path = "updateSharingCodeExpireTime"
            )

        val expectedExpiredAt = Instant.ofEpochSecond(expiredAt.epochSecond)
        coVerify(exactly = 1) {
            service.updateSharingCodeExpireTime("sharingCode1", expectedExpiredAt, "user1")
        }
    }

    @Test
    fun `updateSharingCodeExpireTime - invalid expired at`() {
        val expiredAt = OffsetDateTime.now().minusDays(1).toInstant()

        graphqlTester.withUser(id = "user1")
            .document("""
                mutation {
                    updateSharingCodeExpireTime(id: "sharingCode1", expiredAt: ${expiredAt.epochSecond}) {
                        id, publicCode, sharingUrl, created, expiredAt
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.BAD_REQUEST,
                code = "common.validation",
                extensions = mapOf("invalidProperties" to listOf("expiredAt")),
                path = "updateSharingCodeExpireTime"
            )

        coVerify(exactly = 0) { service.updateSharingCodeExpireTime(any(), any(), any()) }
    }

    @Test
    fun `updateSharingCodeExpireTime - unauthenticated`() {
        val expiredAt = Instant.now().plusSeconds(100)

        graphqlTester
            .document("""
                mutation {
                    updateSharingCodeExpireTime(id: "sharingCode1", expiredAt: ${expiredAt.epochSecond}) {
                        id, publicCode, sharingUrl, created, expiredAt
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.FORBIDDEN,
                code = "common.access_denied",
                path = "updateSharingCodeExpireTime"
            )

        coVerify(exactly = 0) { service.updateSharingCodeExpireTime(any(), any(), any()) }
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
    fun `deleteSharingCodeById - not found`() {
        coEvery { service.deleteSharingCodeById("sharingCode1", "user1") } throws
                HeartBeatSharingNotFoundByIdException("sharingCode1")

        graphqlTester.withUser(id = "user1")
            .document("""mutation { deleteSharingCodeById(id: "sharingCode1") }""")
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.NOT_FOUND,
                code = "heart_beat_sharing.not_found.by_id",
                extensions = mapOf("id" to "sharingCode1"),
                path = "deleteSharingCodeById"
            )

        coVerify(exactly = 1) { service.deleteSharingCodeById("sharingCode1", "user1") }
    }

    @Test
    fun `deleteSharingCodeById - unauthenticated`() {
        graphqlTester
            .document("""mutation { deleteSharingCodeById(id: "sharingCode1") }""")
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.FORBIDDEN,
                code = "common.access_denied",
                path = "deleteSharingCodeById"
            )

        coVerify(exactly = 0) { service.deleteSharingCodeById(any(), any()) }
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
    fun `getProfileSharingCode - validation error`() {
        coEvery { userService.getUserById(any()) } returns User(
            id = "userId",
            displayName = null,
            email = null,
            emailVerified = false
        )

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getProfile {
                        sharingCodes(page: -1, size: 100, sort: CREATED_ASC) {
                            content { id, publicCode, sharingUrl, created, expiredAt }
                            pageInfo { totalPages, totalItems, hasNext }
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.BAD_REQUEST,
                code = "common.validation",
                path = "getProfile.sharingCodes",
                extensions = mapOf(
                    "invalidProperties" to listOf("size", "page")
                )
            )

        coVerify(exactly = 0) { service.getSharingCodesByUserId(any(), any()) }
    }
}