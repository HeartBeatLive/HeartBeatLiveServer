package com.munoon.heartbeatlive.server.user.controller

import com.munoon.heartbeatlive.server.AbstractGraphqlHttpTest
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.UserMapper.asGraphqlProfile
import com.munoon.heartbeatlive.server.user.UserRole
import com.munoon.heartbeatlive.server.user.model.GraphqlProfileTo
import com.munoon.heartbeatlive.server.user.model.UpdateUserInfoFromJwtTo
import com.munoon.heartbeatlive.server.user.service.UserService
import com.munoon.heartbeatlive.server.utils.AuthTestUtils.withUser
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.expectSingleUnauthenticatedError
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.expectSingleValidationError
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.isEqualsTo
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.satisfyNoErrors
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
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
    fun `checkEmailReserved - email invalid`() {
        graphqlTester.document("query { checkEmailReserved(email: \"invalid_email\") }")
            .execute()
            .errors().expectSingleValidationError("checkEmailReserved", "email")

        coVerify(exactly = 0) { userService.checkEmailReserved("email@gmail.com") }
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
    fun `updateProfileDisplayName - invalid length`() {
        graphqlTester.withUser()
            .document("""
                mutation {
                    updateProfileDisplayName(displayName: "A") {
                        id, displayName, email, emailVerified, roles
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleValidationError("updateProfileDisplayName", "displayName")

        coVerify(exactly = 0) { userService.updateUserDisplayName(any(), any()) }
    }

    @Test
    fun `updateProfileDisplayName - user not authenticated`() {
        graphqlTester
            .document("""
                mutation {
                    updateProfileDisplayName(displayName: "Display Name") {
                        id, displayName, email, emailVerified, roles
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleUnauthenticatedError(path = "updateProfileDisplayName")

        coVerify(exactly = 0) { userService.updateUserDisplayName(any(), any()) }
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
    fun `updateProfileInfo - user not authenticated`() {
        graphqlTester
            .document("""
                mutation {
                    updateProfileInfo {
                        id, displayName, email, emailVerified, roles
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleUnauthenticatedError(path = "updateProfileInfo")

        coVerify(exactly = 0) { userService.updateUserInfoFromJwt(any(), any()) }
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
    fun `getProfile - user not authenticated`() {
        graphqlTester
            .document("""
                query {
                    getProfile { id, displayName, email, emailVerified, roles }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleUnauthenticatedError(path = "getProfile")

        coVerify(exactly = 0) { userService.getUserById(any()) }
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
    fun `deleteProfile - user not authenticated`() {
        graphqlTester
            .document("mutation { deleteProfile }")
            .execute()
            .errors().expectSingleUnauthenticatedError(path = "deleteProfile")

        coVerify(exactly = 0) { userService.deleteUserById(any(), any()) }
    }
}