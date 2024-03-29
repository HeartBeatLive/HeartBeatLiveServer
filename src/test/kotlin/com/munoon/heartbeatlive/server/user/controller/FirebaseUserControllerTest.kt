package com.munoon.heartbeatlive.server.user.controller

import com.munoon.heartbeatlive.server.AbstractGraphqlHttpTest
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.model.GraphqlFirebaseCreateUserInput
import com.munoon.heartbeatlive.server.user.service.UserService
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.expectSingleUnauthenticatedError
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.isEqualsTo
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.satisfyNoErrors
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders

@SpringBootTest(properties = ["auth.firebase.function.token=${FirebaseUserControllerTest.FIREBASE_FUNCTION_TOKEN}"])
internal class FirebaseUserControllerTest : AbstractGraphqlHttpTest() {
    companion object {
        const val FIREBASE_FUNCTION_TOKEN = "super-secret-firebase-function-token"
    }

    @MockkBean
    private lateinit var userService: UserService

    @Test
    fun firebaseCreateUser() {
        val request = GraphqlFirebaseCreateUserInput("1", "email@gmail.com", true)
        coEvery { userService.createUser(any()) } returns User(id = "1", null, null, false)

        graphqlTester.mutate()
            .header(HttpHeaders.AUTHORIZATION, FIREBASE_FUNCTION_TOKEN)
            .build()
            .document("mutation(\$request: FirebaseCreateUserRequest!) { firebaseCreateUser(request: \$request) }")
            .variable("request", request)
            .execute()
            .satisfyNoErrors()
            .path("firebaseCreateUser").isEqualsTo(true)

        coVerify(exactly = 1) { userService.createUser(request) }
    }

    @Test
    fun `firebaseCreateUser - access denied`() {
        val request = GraphqlFirebaseCreateUserInput("1", "email@gmail.com", true)

        graphqlTester
            .document("mutation(\$request: FirebaseCreateUserRequest!) { firebaseCreateUser(request: \$request) }")
            .variable("request", request)
            .execute()
            .errors().expectSingleUnauthenticatedError(path = "firebaseCreateUser")

        coVerify(exactly = 0) { userService.createUser(any()) }
    }

    @Test
    fun firebaseDeleteUser() {
        coEvery { userService.deleteUserById("1", updateFirebaseState = false) } returns Unit

        graphqlTester.mutate()
            .header(HttpHeaders.AUTHORIZATION, FIREBASE_FUNCTION_TOKEN)
            .build()
            .document("mutation { firebaseDeleteUser(userId: \"1\") }")
            .execute()
            .satisfyNoErrors()
            .path("firebaseDeleteUser").isEqualsTo(true)

        coVerify(exactly = 1) { userService.deleteUserById("1", updateFirebaseState = false) }
    }

    @Test
    fun `firebaseDeleteUser - access denied`() {
        graphqlTester
            .document("mutation { firebaseDeleteUser(userId: \"1\") }")
            .execute()
            .errors().expectSingleUnauthenticatedError(path = "firebaseDeleteUser")

        coVerify(exactly = 0) { userService.deleteUserById("1", updateFirebaseState = false) }
    }
}