package com.munoon.heartbeatlive.server.push.controller

import com.munoon.heartbeatlive.server.AbstractGraphqlHttpTest
import com.munoon.heartbeatlive.server.push.model.GraphqlOneSignalPushNotificationConnectionDetails
import com.munoon.heartbeatlive.server.utils.AuthTestUtils.withUser
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.expectSingleUnauthenticatedError
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.isEqualsTo
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.satisfyNoErrors
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(properties = [
    "one-signal.rest-api-key=${OneSignalPushNotificationConnectionDetailsControllerTest.TEST_REST_API_KEY}",
    "one-signal.app-id=${OneSignalPushNotificationConnectionDetailsControllerTest.TEST_APP_ID}"
])
internal class OneSignalPushNotificationConnectionDetailsControllerTest : AbstractGraphqlHttpTest() {
    companion object {
        const val TEST_REST_API_KEY = "random-test-api-key"
        const val TEST_APP_ID = "one-signal-app-id"
    }

    @Test
    fun getOneSignalPushNotificationConnectionDetails() {
        val expectedResponse = GraphqlOneSignalPushNotificationConnectionDetails(TEST_APP_ID)

        graphqlTester.document("query { getOneSignalPushNotificationConnectionDetails { appId } }")
            .execute()
            .satisfyNoErrors()
            .path("getOneSignalPushNotificationConnectionDetails").isEqualsTo(expectedResponse)
    }

    @Test
    fun getOneSignalIdentifierAuthenticationToken() {
        graphqlTester.withUser(id = "user1")
            .document("query { getOneSignalPushNotificationConnectionDetails { identifierAuthenticationToken } }")
            .execute()
            .satisfyNoErrors()
            .path("getOneSignalPushNotificationConnectionDetails.identifierAuthenticationToken")
            .isEqualsTo("bb8c88f289d22e262b7f43f4074bac015aceb4722a004c73503f733bc674e77d")
    }

    @Test
    fun `getOneSignalIdentifierAuthenticationToken - not authenticated`() {
        graphqlTester
            .document("query { getOneSignalPushNotificationConnectionDetails { identifierAuthenticationToken } }")
            .execute()
            .errors().expectSingleUnauthenticatedError(
                path = "getOneSignalPushNotificationConnectionDetails.identifierAuthenticationToken")
    }
}