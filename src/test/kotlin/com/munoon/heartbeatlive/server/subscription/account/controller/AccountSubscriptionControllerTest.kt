package com.munoon.heartbeatlive.server.subscription.account.controller

import com.munoon.heartbeatlive.server.AbstractGraphqlHttpTest
import com.munoon.heartbeatlive.server.subscription.account.PaymentProviderNotFoundException
import com.munoon.heartbeatlive.server.subscription.account.model.GraphqlPaymentProviderName
import com.munoon.heartbeatlive.server.subscription.account.model.StripePaymentProviderInfo
import com.munoon.heartbeatlive.server.subscription.account.service.AccountSubscriptionService
import com.munoon.heartbeatlive.server.utils.AuthTestUtils.withUser
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.expectSingleError
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.expectSingleUnauthenticatedError
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.isEqualsTo
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.satisfyNoErrors
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.execution.ErrorType

@SpringBootTest
internal class AccountSubscriptionControllerTest : AbstractGraphqlHttpTest() {
    @MockkBean
    private lateinit var service: AccountSubscriptionService

    @Test
    fun getPaymentProvider() {
        val stripePublicKey = "stripePublicKey"
        every { service.getPaymentProviderInfo(any()) } returns StripePaymentProviderInfo(stripePublicKey)

        graphqlTester.document("""
            query(${'$'}supportedProviders: [PaymentProviderName!]!) {
                getPaymentProvider(supportedProviders: ${'$'}supportedProviders) {
                    __typename,
                    name,
                    ... on StripePaymentProvider {
                        publicApiKey
                    }
                }
            }
        """.trimIndent())
            .variable("supportedProviders", setOf(GraphqlPaymentProviderName.STRIPE.name))
            .execute()
            .satisfyNoErrors()
            .path("getPaymentProvider.__typename").isEqualsTo("StripePaymentProvider")
            .path("getPaymentProvider.name").isEqualsTo(GraphqlPaymentProviderName.STRIPE.name)
            .path("getPaymentProvider.publicApiKey").isEqualsTo(stripePublicKey)
    }

    @Test
    fun `getPaymentProvider - payment provider not found`() {
        every { service.getPaymentProviderInfo(any()) } throws PaymentProviderNotFoundException()

        graphqlTester.document("""
            query(${'$'}supportedProviders: [PaymentProviderName!]!) {
                getPaymentProvider(supportedProviders: ${'$'}supportedProviders) {
                    __typename,
                    name,
                    ... on StripePaymentProvider {
                        publicApiKey
                    }
                }
            }
        """.trimIndent())
            .variable("supportedProviders", setOf(GraphqlPaymentProviderName.STRIPE.name))
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.NOT_FOUND,
                code = "payment.provider.not_found",
                path = "getPaymentProvider"
            )
    }

    @Test
    fun stopRenewingSubscription() {
        coEvery { service.stopRenewingSubscription(any()) } returns Unit

        graphqlTester.withUser(id = "user1")
            .document("mutation { stopRenewingSubscription }")
            .execute()
            .satisfyNoErrors()
            .path("stopRenewingSubscription").isEqualsTo(true)

        coVerify(exactly = 1) { service.stopRenewingSubscription("user1") }
    }

    @Test
    fun `stopRenewingSubscription - not authenticated`() {
        graphqlTester
            .document("mutation { stopRenewingSubscription }")
            .execute()
            .errors().expectSingleUnauthenticatedError(path = "stopRenewingSubscription")

        coVerify(exactly = 0) { service.stopRenewingSubscription(any()) }
    }
}