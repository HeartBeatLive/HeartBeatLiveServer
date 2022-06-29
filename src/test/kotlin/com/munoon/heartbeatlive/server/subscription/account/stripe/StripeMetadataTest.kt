package com.munoon.heartbeatlive.server.subscription.account.stripe

import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.Duration

class StripeMetadataTest : FreeSpec({
    "Subscription" - {
        "Refund Duration" - {
            "read" - {
                "ok" {
                    val metadata = mapOf(StripeMetadata.Subscription.REFUND_DURATION.key to "60")
                    StripeMetadata.Subscription.REFUND_DURATION.getValue(metadata) shouldBe Duration.ofMinutes(1)
                }

                "not long" {
                    val metadata = mapOf(StripeMetadata.Subscription.REFUND_DURATION.key to "abc")
                    StripeMetadata.Subscription.REFUND_DURATION.getValue(metadata).shouldBeNull()
                }

                "not found" {
                    StripeMetadata.Subscription.REFUND_DURATION.getValue(emptyMap()).shouldBeNull()
                }
            }

            "write" {
                val expected = StripeMetadata.Subscription.REFUND_DURATION.key to "60"
                StripeMetadata.Subscription.REFUND_DURATION.addValue(Duration.ofMinutes(1)) shouldBe expected
            }
        }

        "Subscription Plan" - {
            "read" - {
                "ok" {
                    val metadata = mapOf(StripeMetadata.Subscription.SUBSCRIPTION_PLAN.key to "PRO")
                    StripeMetadata.Subscription.SUBSCRIPTION_PLAN.getValue(metadata) shouldBe UserSubscriptionPlan.PRO
                }

                "unknown" {
                    val metadata = mapOf(StripeMetadata.Subscription.SUBSCRIPTION_PLAN.key to "abc")
                    StripeMetadata.Subscription.SUBSCRIPTION_PLAN.getValue(metadata).shouldBeNull()
                }

                "not found" {
                    StripeMetadata.Subscription.SUBSCRIPTION_PLAN.getValue(emptyMap()).shouldBeNull()
                }
            }

            "write" {
                val expected = StripeMetadata.Subscription.SUBSCRIPTION_PLAN.key to "PRO"
                StripeMetadata.Subscription.SUBSCRIPTION_PLAN.addValue(UserSubscriptionPlan.PRO) shouldBe expected
            }
        }

        "User Id" - {
            "read" - {
                "ok" {
                    val metadata = mapOf(StripeMetadata.Subscription.USER_ID.key to "user1")
                    StripeMetadata.Subscription.USER_ID.getValue(metadata) shouldBe "user1"
                }

                "not found" {
                    StripeMetadata.Subscription.USER_ID.getValue(emptyMap()).shouldBeNull()
                }
            }

            "write" {
                val expected = StripeMetadata.Subscription.USER_ID.key to "user1"
                StripeMetadata.Subscription.USER_ID.addValue("user1") shouldBe expected
            }
        }
    }

    "Customer" - {
        "User Id" - {
            "read" - {
                "ok" {
                    val metadata = mapOf(StripeMetadata.Customer.USER_ID.key to "user1")
                    StripeMetadata.Customer.USER_ID.getValue(metadata) shouldBe "user1"
                }

                "not found" {
                    StripeMetadata.Customer.USER_ID.getValue(emptyMap()).shouldBeNull()
                }
            }

            "write" {
                val expected = StripeMetadata.Customer.USER_ID.key to "user1"
                StripeMetadata.Customer.USER_ID.addValue("user1") shouldBe expected
            }
        }
    }
})
