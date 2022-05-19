package com.munoon.heartbeatlive.server.sharing.controller

import com.munoon.heartbeatlive.server.AbstractGraphqlHttpTest
import com.munoon.heartbeatlive.server.ban.service.UserBanService
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharing
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharingExpiredException
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharingNotFoundByPublicCodeException
import com.munoon.heartbeatlive.server.sharing.service.HeartBeatSharingService
import com.munoon.heartbeatlive.server.subscription.service.SubscriptionService
import com.munoon.heartbeatlive.server.utils.AuthTestUtils.withUser
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.expectSingleError
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.isEqualsTo
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.satisfyNoErrors
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.execution.ErrorType
import java.time.Instant
import java.time.OffsetDateTime

@SpringBootTest
internal class HeartBeatSharingPublicControllerTest : AbstractGraphqlHttpTest() {
    @MockkBean
    private lateinit var heartBeatSharingService: HeartBeatSharingService

    @MockkBean
    private lateinit var subscriptionService: SubscriptionService

    @MockkBean
    private lateinit var userBanService: UserBanService

    @Test
    fun getSharingCodeByPublicCode() {
        val created = Instant.now()
        val expiredAt = OffsetDateTime.now()
            .plusDays(10)
            .withSecond(0)
            .withNano(0)
            .toInstant()

        coEvery { heartBeatSharingService.getSharingCodeByPublicCode("ABC123") } returns HeartBeatSharing(
            id = "sharingCode1",
            publicCode = "ABC123",
            userId = "user1",
            created = created,
            expiredAt = expiredAt
        )
        coEvery { subscriptionService.checkUserHaveMaximumSubscribers("user1") } returns false

        graphqlTester.document("""
            query {
                getSharingCodeByPublicCode(publicCode: "ABC123") {
                    publicCode,
                    sharingUrl,
                    created,
                    expiredAt
                }
            }
        """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getSharingCodeByPublicCode.publicCode").isEqualsTo("ABC123")
            .path("getSharingCodeByPublicCode.sharingUrl").isEqualsTo("https://heartbeatlive.com/sharing/ABC123")
            .path("getSharingCodeByPublicCode.created").isEqualsTo(created.epochSecond)
            .path("getSharingCodeByPublicCode.expiredAt").isEqualsTo(expiredAt.epochSecond)

        coVerify(exactly = 1) { heartBeatSharingService.getSharingCodeByPublicCode("ABC123") }
        coVerify(exactly = 1) { subscriptionService.checkUserHaveMaximumSubscribers("user1") }
    }

    @Test
    fun `getSharingCodeByPublicCode - not found`() {
        coEvery { heartBeatSharingService.getSharingCodeByPublicCode("ABC123") } throws
                HeartBeatSharingNotFoundByPublicCodeException("ABC123")

        graphqlTester.document("""
            query {
                getSharingCodeByPublicCode(publicCode: "ABC123") {
                    publicCode,
                    sharingUrl,
                    created,
                    expiredAt
                }
            }
        """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.NOT_FOUND,
                code = "heart_beat_sharing.not_found.by_public_code",
                extensions = mapOf("publicCode" to "ABC123"),
                path = "getSharingCodeByPublicCode"
            )

        coVerify(exactly = 1) { heartBeatSharingService.getSharingCodeByPublicCode("ABC123") }
    }

    @Test
    fun `getSharingCodeByPublicCode - invalid public code`() {
        graphqlTester.document("""
            query {
                getSharingCodeByPublicCode(publicCode: "") {
                    publicCode,
                    sharingUrl,
                    created,
                    expiredAt
                }
            }
        """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.BAD_REQUEST,
                code = "common.validation",
                extensions = mapOf("invalidProperties" to listOf("publicCode")),
                path = "getSharingCodeByPublicCode"
            )

        coVerify(exactly = 0) { heartBeatSharingService.getSharingCodeByPublicCode(any()) }
    }

    @Test
    fun `getSharingCodeByPublicCode - public code expired`() {
        coEvery { heartBeatSharingService.getSharingCodeByPublicCode("ABC123") } throws
                HeartBeatSharingExpiredException()

        graphqlTester.document("""
            query {
                getSharingCodeByPublicCode(publicCode: "ABC123") {
                    publicCode,
                    sharingUrl,
                    created,
                    expiredAt
                }
            }
        """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.FORBIDDEN,
                code = "heart_beat_sharing.expired",
                path = "getSharingCodeByPublicCode"
            )

        coVerify(exactly = 1) { heartBeatSharingService.getSharingCodeByPublicCode("ABC123") }
    }

    @Test
    fun `getSharingCodeByPublicCode - user has no more free subscribers`() {
        coEvery { heartBeatSharingService.getSharingCodeByPublicCode("ABC123") } returns HeartBeatSharing(
            id = "sharing1",
            publicCode = "ABC123",
            userId = "user1",
            expiredAt = null
        )
        coEvery { subscriptionService.checkUserHaveMaximumSubscribers("user1") } returns true

        graphqlTester.document("""
            query {
                getSharingCodeByPublicCode(publicCode: "ABC123") {
                    publicCode,
                    sharingUrl,
                    created,
                    expiredAt
                }
            }
        """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.FORBIDDEN,
                code = "subscription.subscribers_limit_exceeded",
                path = "getSharingCodeByPublicCode"
            )

        coVerify(exactly = 1) { heartBeatSharingService.getSharingCodeByPublicCode("ABC123") }
        coVerify(exactly = 1) { subscriptionService.checkUserHaveMaximumSubscribers("user1") }
    }

    @Test
    fun `getSharingCodeByPublicCode - authenticated user is banned by sharing code owner`() {
        coEvery { heartBeatSharingService.getSharingCodeByPublicCode("ABC123") } returns HeartBeatSharing(
            id = "sharing1",
            publicCode = "ABC123",
            userId = "user1",
            expiredAt = null
        )
        coEvery { subscriptionService.checkUserHaveMaximumSubscribers("user1") } returns false
        coEvery { userBanService.checkUserBanned("user2", bannedByUserId = "user1") } returns true

        graphqlTester.withUser(id = "user2").document("""
            query {
                getSharingCodeByPublicCode(publicCode: "ABC123") {
                    publicCode,
                    sharingUrl,
                    created,
                    expiredAt
                }
            }
        """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.FORBIDDEN,
                code = "ban.banned",
                path = "getSharingCodeByPublicCode"
            )

        coVerify(exactly = 1) { heartBeatSharingService.getSharingCodeByPublicCode("ABC123") }
        coVerify(exactly = 1) { subscriptionService.checkUserHaveMaximumSubscribers("user1") }
        coVerify(exactly = 1) { userBanService.checkUserBanned("user2", bannedByUserId = "user1") }
    }
}