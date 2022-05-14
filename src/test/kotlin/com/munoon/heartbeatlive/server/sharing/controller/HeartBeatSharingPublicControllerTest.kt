package com.munoon.heartbeatlive.server.sharing.controller

import com.munoon.heartbeatlive.server.AbstractGraphqlHttpTest
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharing
import com.munoon.heartbeatlive.server.sharing.service.HeartBeatSharingService
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.model.GraphqlPublicProfileTo
import com.munoon.heartbeatlive.server.user.service.UserService
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.isEqualsTo
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.satisfyNoErrors
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant
import java.time.OffsetDateTime

@SpringBootTest
internal class HeartBeatSharingPublicControllerTest : AbstractGraphqlHttpTest() {
    @MockkBean
    private lateinit var heartBeatSharingService: HeartBeatSharingService

    @MockkBean
    private lateinit var userService: UserService

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
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `getSharingCodeByPublicCode - not found`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `getSharingCodeByPublicCode - invalid public code`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `getSharingCodeByPublicCode - public code expired`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `getSharingCodeByPublicCode - user has no more free subscribers`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    fun mapSharingCodeUser() {
        coEvery { heartBeatSharingService.getSharingCodeByPublicCode("ABC123") } returns HeartBeatSharing(
            id = "sharingCode1",
            publicCode = "ABC123",
            userId = "user1",
            expiredAt = null
        )
        every { userService.getUsersByIds(any()) } returns flowOf(User(
            id = "user1",
            displayName = "Test User",
            email = null,
            emailVerified = false
        ))

        graphqlTester.document("""
            query {
                getSharingCodeByPublicCode(publicCode: "ABC123") {
                    user { displayName }
                }
            }
        """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getSharingCodeByPublicCode.user").isEqualsTo(GraphqlPublicProfileTo("Test User"))

        coVerify(exactly = 1) { heartBeatSharingService.getSharingCodeByPublicCode("ABC123") }
        verify(exactly = 1) { userService.getUsersByIds(setOf("user1")) }
    }
}