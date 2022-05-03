package com.munoon.heartbeatlive.server.user.controller

import com.munoon.heartbeatlive.server.AbstractGraphqlHttpTest
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.UserRole
import com.munoon.heartbeatlive.server.user.asGraphqlProfile
import com.munoon.heartbeatlive.server.user.model.GraphqlProfileTo
import com.munoon.heartbeatlive.server.user.model.UpdateUserInfoFromJwtTo
import com.munoon.heartbeatlive.server.user.service.UserService
import com.munoon.heartbeatlive.server.utils.AuthTestUtils.withUser
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.isEqualsTo
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.satisfyNoErrors
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
internal class ProfileControllerTest : AbstractGraphqlHttpTest() {
    @MockkBean
    private lateinit var userService: UserService

    @Test
    fun `checkEmailReserved - true`() {
        coEvery { userService.checkEmailReserved("email@gmail.com") }.returns(true)

        graphqlTester.document("query { checkEmailReserved(email: \"email@gmail.com\") }")
            .execute()
            .satisfyNoErrors()
            .path("checkEmailReserved").entity(Boolean::class.java).isEqualTo(true)
    }

    @Test
    fun `checkEmailReserved - false`() {
        coEvery { userService.checkEmailReserved("email@gmail.com") }.returns(false)

        graphqlTester.document("query { checkEmailReserved(email: \"email@gmail.com\") }")
            .execute()
            .satisfyNoErrors()
            .path("checkEmailReserved").entity(Boolean::class.java).isEqualTo(false)
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `checkEmailReserved - user authenticated`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `checkEmailReserved - email invalid`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    fun updateProfileDisplayName() {
        val user = User(
            id = "1",
            displayName = "New Name",
            email = "email@example.com",
            emailVerified = true,
            roles = setOf(UserRole.ADMIN)
        )
        coEvery { userService.updateUserDisplayName("1", "New Name") }
            .returns(user)

        graphqlTester.withUser()
            .document("""
                mutation {
                    updateProfileDisplayName(displayName: "New Name") {
                        id, displayName, email, emailVerified, roles
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("updateProfileDisplayName").entity(GraphqlProfileTo::class.java).isEqualTo(user.asGraphqlProfile())

        coVerify(exactly = 1) { userService.updateUserDisplayName("1", "New Name") }
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `updateProfileDisplayName - invalid length`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `updateProfileDisplayName - user not authenticated`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    fun updateProfileInfo() {
        val user = User(
            id = "1",
            displayName = "New Name",
            email = "email@example.com",
            emailVerified = true,
            roles = setOf(UserRole.ADMIN)
        )
        coEvery { userService.updateUserInfoFromJwt("1", any()) } returns user

        graphqlTester.withUser(emailVerified = true)
            .document("""
                mutation {
                    updateProfileInfo {
                        id, displayName, email, emailVerified, roles
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("updateProfileInfo").isEqualsTo(user.asGraphqlProfile())

        val expectedUpdateUserInfo = UpdateUserInfoFromJwtTo(emailVerified = true)
        coVerify(exactly = 1) { userService.updateUserInfoFromJwt("1", expectedUpdateUserInfo) }
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `updateProfileInfo - user not authenticated`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    fun getProfile() {
        val user = User(
            id = "1",
            displayName = "Display Name",
            email = "email@example.com",
            emailVerified = true,
            roles = setOf(UserRole.ADMIN)
        )
        coEvery { userService.getUserById("1") }.returns(user)

        graphqlTester.withUser()
            .document("""
                query {
                    getProfile { id, displayName, email, emailVerified, roles }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getProfile").entity(GraphqlProfileTo::class.java).isEqualTo(user.asGraphqlProfile())

        coVerify(exactly = 1) { userService.getUserById("1") }
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `getProfile - user not authenticated`() {
        // TODO impl this when error schema will be ready
    }

    @Test
    fun deleteProfile() {
        coEvery { userService.deleteUserById("1", updateFirebaseState = true) } returns Unit

        graphqlTester.withUser()
            .document("mutation { deleteProfile }")
            .execute()
            .satisfyNoErrors()
            .path("deleteProfile").isEqualsTo(true)

        coVerify(exactly = 1) { userService.deleteUserById("1", updateFirebaseState = true) }
    }

    @Test
    @Disabled("Test will be implemented when error schema will be specified")
    fun `deleteProfile - user not authenticated`() {
        // TODO impl this when error schema will be ready
    }
}