package com.munoon.heartbeatlive.server.ban.controller

import com.munoon.heartbeatlive.server.AbstractGraphqlHttpTest
import com.munoon.heartbeatlive.server.ban.UserBan
import com.munoon.heartbeatlive.server.ban.service.UserBanService
import com.munoon.heartbeatlive.server.common.PageResult
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.model.GraphqlPublicProfileTo
import com.munoon.heartbeatlive.server.user.service.UserService
import com.munoon.heartbeatlive.server.utils.AuthTestUtils.withUser
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.isEqualsTo
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.satisfyNoErrors
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
internal class UserBanUserInfoControllerTest : AbstractGraphqlHttpTest() {
    @MockkBean
    private lateinit var userBanService: UserBanService

    @MockkBean
    private lateinit var userService: UserService

    @Test
    fun user() {
        coEvery { userBanService.getBannedUsers(any(), any()) } returns PageResult(
            data = flowOf(
                UserBan(
                    id = "ban1",
                    userId = "user1",
                    bannedUserId = "user2"
                )
            ),
            totalItemsCount = 2
        )
        coEvery { userService.getUsersByIds(setOf("user1")) } returns flowOf(User(
            id = "user1",
            displayName = "User 1",
            email = null,
            emailVerified = false
        ))

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getBannedUsers(page: 0, size: 10) {
                        content {
                            user { displayName }
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getBannedUsers.content[0].user").isEqualsTo(GraphqlPublicProfileTo("User 1"))

        coVerify(exactly = 1) { userService.getUsersByIds(setOf("user1")) }
    }

    @Test
    fun bannedUser() {
        coEvery { userBanService.getBannedUsers(any(), any()) } returns PageResult(
            data = flowOf(
                UserBan(
                    id = "ban1",
                    userId = "user1",
                    bannedUserId = "user2"
                )
            ),
            totalItemsCount = 2
        )
        coEvery { userService.getUsersByIds(setOf("user2")) } returns flowOf(User(
            id = "user2",
            displayName = "User 2",
            email = null,
            emailVerified = false
        ))

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getBannedUsers(page: 0, size: 10) {
                        content {
                            bannedUser { displayName }
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getBannedUsers.content[0].bannedUser").isEqualsTo(GraphqlPublicProfileTo("User 2"))

        coVerify(exactly = 1) { userService.getUsersByIds(setOf("user2")) }
    }
}