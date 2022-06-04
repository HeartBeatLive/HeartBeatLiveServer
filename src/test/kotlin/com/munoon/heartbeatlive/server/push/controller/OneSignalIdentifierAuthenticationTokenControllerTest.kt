package com.munoon.heartbeatlive.server.push.controller

import com.munoon.heartbeatlive.server.AbstractGraphqlHttpTest
import com.munoon.heartbeatlive.server.utils.AuthTestUtils.withUser
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.expectSingleUnauthenticatedError
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.isEqualsTo
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.satisfyNoErrors
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(properties = [
    "one-signal.rest-api-key=${OneSignalIdentifierAuthenticationTokenControllerTest.TEST_REST_API_KEY}"
])
internal class OneSignalIdentifierAuthenticationTokenControllerTest : AbstractGraphqlHttpTest() {
    companion object {
        const val TEST_REST_API_KEY = "random-test-api-key"
    }

    @Test
    fun getOneSignalIdentifierAuthenticationToken() {
        graphqlTester.withUser(id = "user1")
            .document("query { getOneSignalIdentifierAuthenticationToken }")
            .execute()
            .satisfyNoErrors()
            .path("getOneSignalIdentifierAuthenticationToken")
            .isEqualsTo("bb8c88f289d22e262b7f43f4074bac015aceb4722a004c73503f733bc674e77d")
    }

    @Test
    fun `getOneSignalIdentifierAuthenticationToken - not authenticated`() {
        graphqlTester.document("query { getOneSignalIdentifierAuthenticationToken }")
            .execute()
            .errors().expectSingleUnauthenticatedError(path = "getOneSignalIdentifierAuthenticationToken")
    }
}