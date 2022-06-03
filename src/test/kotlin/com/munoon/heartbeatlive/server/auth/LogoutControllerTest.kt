package com.munoon.heartbeatlive.server.auth

import com.munoon.heartbeatlive.server.AbstractGraphqlHttpTest
import com.munoon.heartbeatlive.server.utils.AuthTestUtils.withUser
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.expectSingleUnauthenticatedError
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.satisfyNoErrors
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
internal class LogoutControllerTest : AbstractGraphqlHttpTest() {
    @Test
    fun logout() {
        graphqlTester
            .withUser()
            .document("mutation { logout }")
            .execute()
            .satisfyNoErrors()
            .path("logout").entity(Boolean::class.java).isEqualTo(true)
    }

    @Test
    fun `logout - access denied`() {
        graphqlTester.document("mutation { logout }")
            .execute()
            .errors().expectSingleUnauthenticatedError(path = "logout")
    }
}