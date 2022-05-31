package com.munoon.heartbeatlive.server.heartrate.controller

import com.munoon.heartbeatlive.server.AbstractGraphqlHttpTest
import com.munoon.heartbeatlive.server.AbstractGraphqlSubscriptionTest
import com.munoon.heartbeatlive.server.heartrate.TooManyHeartRateSubscriptionsExceptions
import com.munoon.heartbeatlive.server.heartrate.model.GraphqlHeartRateInfoTo
import com.munoon.heartbeatlive.server.heartrate.service.HeartRateService
import com.munoon.heartbeatlive.server.utils.AuthTestUtils.withUser
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.expectSingleUnauthenticatedError
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.expectSingleValidationError
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.isEqualsTo
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.satisfyNoErrors
import com.ninjasquad.springmockk.SpykBean
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import reactor.test.StepVerifier
import java.time.Duration

internal class HeartBeatControllerTest {
    @SpringBootTest
    class HeartBeatControllerSendingTest : AbstractGraphqlHttpTest() {
        @SpykBean
        private lateinit var service: HeartRateService

        @Test
        fun sendHeartRate() {
            graphqlTester.withUser()
                .document("mutation { sendHeartRate(data: { heartRate: 123.45 }) }")
                .execute()
                .satisfyNoErrors()
                .path("sendHeartRate").isEqualsTo(true)

            coVerify(exactly = 1) { service.sendHeartRate("1", 123.45f) }
        }

        @Test
        fun `sendHeartRate - invalid`() {
            graphqlTester.withUser()
                .document("mutation { sendHeartRate(data: { heartRate: 500000 }) }")
                .execute()
                .errors().expectSingleValidationError("sendHeartRate", "data.heartRate")

            coVerify(exactly = 0) { service.sendHeartRate(any(), any()) }
        }

        @Test
        fun `sendHeartRate - not authenticated`() {
            graphqlTester
                .document("mutation { sendHeartRate(data: { heartRate: 123.45 }) }")
                .execute()
                .errors().expectSingleUnauthenticatedError(path = "sendHeartRate")

            coVerify(exactly = 0) { service.sendHeartRate(any(), any()) }
        }
    }

    class HeartBeatControllerSubscribeTest : AbstractGraphqlSubscriptionTest() {
        @SpykBean
        private lateinit var service: HeartRateService

        @Test
        fun subscribeToHeartRates() {
            val subscription = graphqlTester.withUser()
                .document("subscription { subscribeToHeartRates { subscriptionId, heartRate, ownHeartRate } }")
                .executeSubscription()
                .toFlux("subscribeToHeartRates", GraphqlHeartRateInfoTo::class.java)

            StepVerifier.create(subscription)
                .thenAwait(Duration.ofSeconds(5)) // wait for initialization
                .then { runBlocking { service.sendHeartRate("1", 123.45f) } }
                .expectNext(GraphqlHeartRateInfoTo(null, 123.45f, true))
                .then { runBlocking { service.sendHeartRate("1", 111.22f) } }
                .expectNext(GraphqlHeartRateInfoTo(null, 111.22f, true))
                .thenCancel()
                .verify(Duration.ofSeconds(30))

            verify(exactly = 1) { service.subscribeToHeartRates("1") }
        }

        @Test
        fun `subscribeToHeartRates - too many subscribers`() {
            every { service.subscribeToHeartRates(any()) } throws
                    TooManyHeartRateSubscriptionsExceptions("1", 15)

            val flux = graphqlTester.withUser()
                .document("subscription { subscribeToHeartRates { subscriptionId, heartRate, ownHeartRate } }")
                .executeSubscription()
                .toFlux("subscribeToHeartRates", GraphqlHeartRateInfoTo::class.java)

            StepVerifier.create(flux)
                .expectError()
                .verify(Duration.ofSeconds(30))

            verify(exactly = 1) { service.subscribeToHeartRates("1") }
        }

        @Test
        fun `subscribeToHeartRates - not authenticated`() {
            val flux = graphqlTester
                .document("subscription { subscribeToHeartRates { subscriptionId, heartRate, ownHeartRate } }")
                .executeSubscription()
                .toFlux("subscribeToHeartRates", GraphqlHeartRateInfoTo::class.java)

            StepVerifier.create(flux)
                .expectError()
                .verify(Duration.ofSeconds(30))

            verify(exactly = 0) { service.subscribeToHeartRates(any()) }
        }
    }
}