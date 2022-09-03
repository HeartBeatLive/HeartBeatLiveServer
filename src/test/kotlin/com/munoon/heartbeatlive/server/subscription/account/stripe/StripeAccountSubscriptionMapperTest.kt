package com.munoon.heartbeatlive.server.subscription.account.stripe

import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeAccountSubscriptionMapper.asGraphql
import com.munoon.heartbeatlive.server.subscription.account.stripe.model.GraphqlStripeRecurringChargeFailureInfo
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
import java.time.Instant

class StripeAccountSubscriptionMapperTest : FreeSpec({
    "Subscription.asGraphql" {
        checkAll(
            5,
            Arb.string(codepoints = Codepoint.alphanumeric(), size = 20),
            Arb.string(codepoints = Codepoint.alphanumeric(), size = 20)
        ) { subscriptionId, subscriptionClientSecret ->
            val expected = GraphqlStripeSubscription(
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

    "StripeRecurringChargeFailure.asGraphql" - {
        "status = REQUIRES_ACTION" {
            val created = Instant.now()
            val expiresAt = Instant.now().plusSeconds(60)

            val expected = GraphqlStripeRecurringChargeFailureInfo(
                createTime = created,
                expireTime = expiresAt,
                clientSecret = "abc",
                failureType = GraphqlStripeRecurringChargeFailureInfo.FailureType.REQUIRES_ACTION
            )

            val stripeRecurringChargeFailure = StripeRecurringChargeFailure(
                userId = "user1",
                stripeInvoiceId = "stripeInvoice1",
                clientSecret = "abc",
                paymentIntentStatus = "requires_action",
                created = created,
                expiresAt = expiresAt
            )

            expected shouldBe stripeRecurringChargeFailure.asGraphql()
        }

        "status = REQUIRES_PAYMENT_METHOD" {
            val created = Instant.now()
            val expiresAt = Instant.now().plusSeconds(60)

            val expected = GraphqlStripeRecurringChargeFailureInfo(
                createTime = created,
                expireTime = expiresAt,
                clientSecret = "abc",
                failureType = GraphqlStripeRecurringChargeFailureInfo.FailureType.REQUIRES_PAYMENT_METHOD
            )

            val stripeRecurringChargeFailure = StripeRecurringChargeFailure(
                userId = "user1",
                stripeInvoiceId = "stripeInvoice1",
                clientSecret = "abc",
                paymentIntentStatus = "requires_payment_method",
                created = created,
                expiresAt = expiresAt
            )

            expected shouldBe stripeRecurringChargeFailure.asGraphql()
        }
    }
})