package com.munoon.heartbeatlive.server.sharing.controller

import com.munoon.heartbeatlive.server.AbstractGraphqlHttpTest
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharing
import com.munoon.heartbeatlive.server.sharing.service.HeartBeatSharingService
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.UserRole
import com.munoon.heartbeatlive.server.user.model.GraphqlProfileTo
import com.munoon.heartbeatlive.server.user.model.GraphqlPublicProfileTo
import com.munoon.heartbeatlive.server.user.service.UserService
import com.munoon.heartbeatlive.server.utils.AuthTestUtils.withUser
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.isEqualsTo
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.satisfyNoErrors
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
internal class HeartBeatSharingUserControllerTest : AbstractGraphqlHttpTest() {
    @MockkBean
    private lateinit var service: HeartBeatSharingService

    @MockkBean
    private lateinit var userService: UserService

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

    @Test
    fun mapPublicSharingCodeUser() {
        coEvery { service.getSharingCodeByPublicCode("ABC123") } returns HeartBeatSharing(
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

        coVerify(exactly = 1) { service.getSharingCodeByPublicCode("ABC123") }
        verify(exactly = 1) { userService.getUsersByIds(setOf("user1")) }
    }
}