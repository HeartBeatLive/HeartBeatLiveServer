package com.munoon.heartbeatlive.server.subscription.account.stripe

import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeAccountSubscriptionMapper.asGraphql
import com.munoon.heartbeatlive.server.subscription.account.stripe.model.GraphqlStripeSubscription
import com.stripe.model.Invoice
import com.stripe.model.PaymentIntent
import com.stripe.model.Subscription
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

class StripeAccountSubscriptionMapperTest : FreeSpec({
    "Subscription.asGraphql" {
        checkAll(
            5,
            Arb.string(codepoints = Codepoint.alphanumeric(), size = 20),
            Arb.string(codepoints = Codepoint.alphanumeric(), size = 20)
        ) { subscriptionId, subscriptionClientSecret ->
            val expected = GraphqlStripeSubscription(
                subscriptionId = subscriptionId,
                clientSecret = subscriptionClientSecret
            )

            val stripeSubscription = Subscription().apply {
                id = subscriptionId
                latestInvoiceObject = Invoice().apply {
                    paymentIntentObject = PaymentIntent().apply {
                        clientSecret = subscriptionClientSecret
                    }
                }
            }

            stripeSubscription.asGraphql() shouldBe expected
        }
    }
})