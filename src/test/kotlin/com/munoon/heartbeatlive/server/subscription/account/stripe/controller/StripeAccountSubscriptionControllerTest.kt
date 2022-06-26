package com.munoon.heartbeatlive.server.subscription.account.stripe.controller

import com.munoon.heartbeatlive.server.AbstractGraphqlHttpTest
import com.munoon.heartbeatlive.server.config.properties.StripeConfigurationProperties
import com.munoon.heartbeatlive.server.config.properties.SubscriptionProperties
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import com.munoon.heartbeatlive.server.subscription.account.stripe.model.GraphqlStripeSubscription
import com.munoon.heartbeatlive.server.subscription.account.stripe.service.StripeAccountSubscriptionService
import com.munoon.heartbeatlive.server.utils.AuthTestUtils.withUser
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.expectSingleError
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.expectSingleUnauthenticatedError
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.isEqualsTo
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.satisfyNoErrors
import com.ninjasquad.springmockk.MockkBean
import com.stripe.model.Invoice
import com.stripe.model.PaymentIntent
import com.stripe.model.Subscription
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.execution.ErrorType

@SpringBootTest
internal class StripeAccountSubscriptionControllerTest : AbstractGraphqlHttpTest() {
    @MockkBean
    private lateinit var service: StripeAccountSubscriptionService

    @MockkBean
    private lateinit var properties: StripeConfigurationProperties

    @Autowired
    private lateinit var subscriptionProperties: SubscriptionProperties

    @BeforeEach
    fun setUpStripeProperties() {
        every { properties.enabled } returns true
    }

    @Test
    fun createStripeSubscription() {
        val expectedStripeSubscription = GraphqlStripeSubscription(
            subscriptionId = "stripeSubscriptionId",
            clientSecret = "stripeClientSecret"
        )

        coEvery { service.createSubscription(any(), any()) } returns Subscription().apply {
            id = "stripeSubscriptionId"
            latestInvoiceObject = Invoice().apply {
                paymentIntentObject = PaymentIntent().apply {
                    clientSecret = "stripeClientSecret"
                }
            }
        }

        val price = subscriptionProperties[UserSubscriptionPlan.PRO].prices.first()
        val priceId = price.getId(UserSubscriptionPlan.PRO)

        graphqlTester.withUser(id = "user1")
            .document("""
                mutation {
                    createStripeSubscription(subscriptionPlanPriceId: "$priceId") {
                        subscriptionId,
                        clientSecret
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("createStripeSubscription").isEqualsTo(expectedStripeSubscription)

        coVerify(exactly = 1) { service.createSubscription(price.stripePriceId!!, "user1") }
    }

    @Test
    fun `createStripeSubscription - unsupported payment provider`() {
        every { properties.enabled } returns false

        graphqlTester.withUser()
            .document("""
                mutation {
                    createStripeSubscription(subscriptionPlanPriceId: "abc") {
                        subscriptionId,
                        clientSecret
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.FORBIDDEN,
                code = "payment.provider.unsupported",
                path = "createStripeSubscription",
                extensions = mapOf("providerName" to "STRIPE")
            )

        coVerify(exactly = 0) { service.createSubscription(any(), any()) }
    }

    @Test
    fun `createStripeSubscription - subscription price not found`() {
        graphqlTester.withUser(id = "user1")
            .document("""
                mutation {
                    createStripeSubscription(subscriptionPlanPriceId: "abc") {
                        subscriptionId,
                        clientSecret
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.NOT_FOUND,
                code = "account_subscription.subscription_plan.price.not_found.by_id",
                path = "createStripeSubscription",
                extensions = mapOf("id" to "abc")
            )

        coVerify(exactly = 0) { service.createSubscription(any(), any()) }
    }

    @Test
    fun `createStripeSubscription - not authenticated`() {
        graphqlTester
            .document("""
                mutation {
                    createStripeSubscription(subscriptionPlanPriceId: "abc") {
                        subscriptionId,
                        clientSecret
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleUnauthenticatedError(path = "createStripeSubscription")

        coVerify(exactly = 0) { service.createSubscription(any(), any()) }
    }
}