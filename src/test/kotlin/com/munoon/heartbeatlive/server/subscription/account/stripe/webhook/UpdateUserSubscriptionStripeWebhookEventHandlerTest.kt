package com.munoon.heartbeatlive.server.subscription.account.stripe.webhook

import com.google.gson.JsonObject
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.service.UserService
import com.stripe.Stripe
import com.stripe.model.Customer
import com.stripe.model.Event
import com.stripe.model.EventData
import com.stripe.model.Invoice
import com.stripe.model.InvoiceLineItem
import com.stripe.model.InvoiceLineItemCollection
import com.stripe.model.InvoiceLineItemPeriod
import com.stripe.net.ApiResource
import io.kotest.core.spec.style.FreeSpec
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.time.Duration
import java.time.Instant
import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeMetadata.Subscription as StripeSubscriptionMeta

internal class UpdateUserSubscriptionStripeWebhookEventHandlerTest : FreeSpec({
    "handleEvent" - {
        "update user subscription" {
            val subscriptionStartTime = Instant.now().epochSecond
            val subscriptionEndTime = Instant.now().plusSeconds(120).epochSecond

            val expectedUserSubscription = User.Subscription(
                plan = UserSubscriptionPlan.PRO,
                expiresAt = Instant.ofEpochSecond(subscriptionEndTime),
                startAt = Instant.ofEpochSecond(subscriptionStartTime),
                refundDuration = Duration.ofDays(3),
                details = User.Subscription.StripeSubscriptionDetails(
                    subscriptionId = "stripeSubscription1",
                    paymentIntentId = "stripePaymentIntent1"
                )
            )

            val event = Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "invoice.paid"
                data = EventData().apply {
                    setObject(Invoice().apply {
                        `object` = "invoice"
                        subscription = "stripeSubscription1"
                        paymentIntent = "stripePaymentIntent1"
                        lines = InvoiceLineItemCollection().apply {
                            data = listOf(
                                InvoiceLineItem().apply {
                                    period = InvoiceLineItemPeriod().apply {
                                        start = subscriptionStartTime
                                        end = subscriptionEndTime
                                    }
                                    metadata = mapOf(
                                        StripeSubscriptionMeta.SUBSCRIPTION_PLAN.addValue(UserSubscriptionPlan.PRO),
                                        StripeSubscriptionMeta.REFUND_DURATION.addValue(Duration.ofDays(3)),
                                        StripeSubscriptionMeta.USER_ID.addValue("user1")
                                    )
                                }
                            )
                        }
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                }
            }

            val userService = mockk<UserService>() {
                coEvery { updateUserSubscription(any(), any()) } returns User(id = "userId", displayName = null,
                    email = null, emailVerified = false)
            }

            ApplicationContextRunner()
                .withBean(UserService::class.java, { userService })
                .withBean(UpdateUserSubscriptionStripeWebhookEventHandler::class.java)
                .run { context ->
                    context.publishEvent(event)

                    coVerify(exactly = 1) { userService.updateUserSubscription("user1", expectedUserSubscription) }
                }
        }

        "ignore as other type" {
            val event = Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "test_type"
                data = EventData().apply {
                    setObject(Invoice().apply {
                        `object` = "invoice"
                        subscription = "stripeSubscription1"
                        paymentIntent = "stripePaymentIntent1"
                        lines = InvoiceLineItemCollection().apply {
                            data = listOf(
                                InvoiceLineItem().apply {
                                    period = InvoiceLineItemPeriod().apply {
                                        start = Instant.now().epochSecond
                                        end = Instant.now().plusSeconds(120).epochSecond
                                    }
                                    metadata = mapOf(
                                        StripeSubscriptionMeta.SUBSCRIPTION_PLAN.addValue(UserSubscriptionPlan.PRO),
                                        StripeSubscriptionMeta.REFUND_DURATION.addValue(Duration.ofDays(3)),
                                        StripeSubscriptionMeta.USER_ID.addValue("userId")
                                    )
                                }
                            )
                        }
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                }
            }

            val userService = mockk<UserService>()

            ApplicationContextRunner()
                .withBean(UserService::class.java, { userService })
                .withBean(UpdateUserSubscriptionStripeWebhookEventHandler::class.java)
                .run { context ->
                    context.publishEvent(event)

                    coVerify(exactly = 0) { userService.updateUserSubscription(any(), any()) }
                }
        }

        "ignore as no invoice info received" {
            val event = Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "invoice.paid"
                data = EventData().apply {
                    setObject(
                        Customer()
                            .apply { `object` = "customer" }
                            .let { ApiResource.GSON.toJsonTree(it) as JsonObject }
                    )
                }
            }

            val userService = mockk<UserService>()

            ApplicationContextRunner()
                .withBean(UserService::class.java, { userService })
                .withBean(UpdateUserSubscriptionStripeWebhookEventHandler::class.java)
                .run { context ->
                    context.publishEvent(event)

                    coVerify(exactly = 0) { userService.updateUserSubscription(any(), any()) }
                }
        }

        "ignore as no invoice subscription received" {
            val event = Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "invoice.paid"
                data = EventData().apply {
                    setObject(Invoice().apply {
                        `object` = "invoice"
                        paymentIntent = "stripePaymentIntent1"
                        lines = InvoiceLineItemCollection().apply {
                            data = listOf(
                                InvoiceLineItem().apply {
                                    period = InvoiceLineItemPeriod().apply {
                                        start = Instant.now().epochSecond
                                        end = Instant.now().plusSeconds(120).epochSecond
                                    }
                                    metadata = mapOf(
                                        StripeSubscriptionMeta.SUBSCRIPTION_PLAN.addValue(UserSubscriptionPlan.PRO),
                                        StripeSubscriptionMeta.REFUND_DURATION.addValue(Duration.ofDays(3)),
                                        StripeSubscriptionMeta.USER_ID.addValue("userId")
                                    )
                                }
                            )
                        }
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                }
            }

            val userService = mockk<UserService>()

            ApplicationContextRunner()
                .withBean(UserService::class.java, { userService })
                .withBean(UpdateUserSubscriptionStripeWebhookEventHandler::class.java)
                .run { context ->
                    context.publishEvent(event)

                    coVerify(exactly = 0) { userService.updateUserSubscription(any(), any()) }
                }
        }

        "ignore as no invoice line item received" {
            val event = Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "invoice.paid"
                data = EventData().apply {
                    setObject(Invoice().apply {
                        `object` = "invoice"
                        subscription = "stripeSubscription1"
                        paymentIntent = "stripePaymentIntent1"
                        lines = InvoiceLineItemCollection()
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                }
            }

            val userService = mockk<UserService>()

            ApplicationContextRunner()
                .withBean(UserService::class.java, { userService })
                .withBean(UpdateUserSubscriptionStripeWebhookEventHandler::class.java)
                .run { context ->
                    context.publishEvent(event)

                    coVerify(exactly = 0) { userService.updateUserSubscription(any(), any()) }
                }
        }

        "ignore as no start period received" {
            val event = Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "invoice.paid"
                data = EventData().apply {
                    setObject(Invoice().apply {
                        `object` = "invoice"
                        subscription = "stripeSubscription1"
                        paymentIntent = "stripePaymentIntent1"
                        lines = InvoiceLineItemCollection().apply {
                            data = listOf(
                                InvoiceLineItem().apply {
                                    period = InvoiceLineItemPeriod().apply {
                                        end = Instant.now().plusSeconds(120).epochSecond
                                    }
                                    metadata = mapOf(
                                        StripeSubscriptionMeta.SUBSCRIPTION_PLAN.addValue(UserSubscriptionPlan.PRO),
                                        StripeSubscriptionMeta.REFUND_DURATION.addValue(Duration.ofDays(3)),
                                        StripeSubscriptionMeta.USER_ID.addValue("userId")
                                    )
                                }
                            )
                        }
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                }
            }

            val userService = mockk<UserService>()

            ApplicationContextRunner()
                .withBean(UserService::class.java, { userService })
                .withBean(UpdateUserSubscriptionStripeWebhookEventHandler::class.java)
                .run { context ->
                    context.publishEvent(event)

                    coVerify(exactly = 0) { userService.updateUserSubscription(any(), any()) }
                }
        }


        "ignore as no end period received" {
            val event = Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "invoice.paid"
                data = EventData().apply {
                    setObject(Invoice().apply {
                        `object` = "invoice"
                        subscription = "stripeSubscription1"
                        paymentIntent = "stripePaymentIntent1"
                        lines = InvoiceLineItemCollection().apply {
                            data = listOf(
                                InvoiceLineItem().apply {
                                    period = InvoiceLineItemPeriod().apply {
                                        start = Instant.now().epochSecond
                                    }
                                    metadata = mapOf(
                                        StripeSubscriptionMeta.SUBSCRIPTION_PLAN.addValue(UserSubscriptionPlan.PRO),
                                        StripeSubscriptionMeta.REFUND_DURATION.addValue(Duration.ofDays(3)),
                                        StripeSubscriptionMeta.USER_ID.addValue("userId")
                                    )
                                }
                            )
                        }
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                }
            }

            val userService = mockk<UserService>()

            ApplicationContextRunner()
                .withBean(UserService::class.java, { userService })
                .withBean(UpdateUserSubscriptionStripeWebhookEventHandler::class.java)
                .run { context ->
                    context.publishEvent(event)

                    coVerify(exactly = 0) { userService.updateUserSubscription(any(), any()) }
                }
        }

        "ignore as no payment intent received" {
            val event = Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "invoice.paid"
                data = EventData().apply {
                    setObject(Invoice().apply {
                        `object` = "invoice"
                        subscription = "stripeSubscription1"
                        lines = InvoiceLineItemCollection().apply {
                            data = listOf(
                                InvoiceLineItem().apply {
                                    period = InvoiceLineItemPeriod().apply {
                                        start = Instant.now().epochSecond
                                        end = Instant.now().plusSeconds(120).epochSecond
                                    }
                                    metadata = mapOf(
                                        StripeSubscriptionMeta.SUBSCRIPTION_PLAN.addValue(UserSubscriptionPlan.PRO),
                                        StripeSubscriptionMeta.REFUND_DURATION.addValue(Duration.ofDays(3)),
                                        StripeSubscriptionMeta.USER_ID.addValue("userId")
                                    )
                                }
                            )
                        }
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                }
            }

            val userService = mockk<UserService>()

            ApplicationContextRunner()
                .withBean(UserService::class.java, { userService })
                .withBean(UpdateUserSubscriptionStripeWebhookEventHandler::class.java)
                .run { context ->
                    context.publishEvent(event)

                    coVerify(exactly = 0) { userService.updateUserSubscription(any(), any()) }
                }
        }

        "ignore as no refund duration metadata received" {
            val event = Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "invoice.paid"
                data = EventData().apply {
                    setObject(Invoice().apply {
                        `object` = "invoice"
                        subscription = "stripeSubscription1"
                        paymentIntent = "stripePaymentIntent1"
                        lines = InvoiceLineItemCollection().apply {
                            data = listOf(
                                InvoiceLineItem().apply {
                                    period = InvoiceLineItemPeriod().apply {
                                        start = Instant.now().epochSecond
                                        end = Instant.now().plusSeconds(120).epochSecond
                                    }
                                    metadata = mapOf(
                                        StripeSubscriptionMeta.SUBSCRIPTION_PLAN.addValue(UserSubscriptionPlan.PRO),
                                        StripeSubscriptionMeta.USER_ID.addValue("userId")
                                    )
                                }
                            )
                        }
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                }
            }

            val userService = mockk<UserService>()

            ApplicationContextRunner()
                .withBean(UserService::class.java, { userService })
                .withBean(UpdateUserSubscriptionStripeWebhookEventHandler::class.java)
                .run { context ->
                    context.publishEvent(event)

                    coVerify(exactly = 0) { userService.updateUserSubscription(any(), any()) }
                }
        }

        "ignore as no subscription plan metadata received" {
            val event = Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "invoice.paid"
                data = EventData().apply {
                    setObject(Invoice().apply {
                        `object` = "invoice"
                        subscription = "stripeSubscription1"
                        paymentIntent = "stripePaymentIntent1"
                        lines = InvoiceLineItemCollection().apply {
                            data = listOf(
                                InvoiceLineItem().apply {
                                    period = InvoiceLineItemPeriod().apply {
                                        start = Instant.now().epochSecond
                                        end = Instant.now().plusSeconds(120).epochSecond
                                    }
                                    metadata = mapOf(
                                        StripeSubscriptionMeta.REFUND_DURATION.addValue(Duration.ofDays(3)),
                                        StripeSubscriptionMeta.USER_ID.addValue("userId")
                                    )
                                }
                            )
                        }
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                }
            }

            val userService = mockk<UserService>()

            ApplicationContextRunner()
                .withBean(UserService::class.java, { userService })
                .withBean(UpdateUserSubscriptionStripeWebhookEventHandler::class.java)
                .run { context ->
                    context.publishEvent(event)

                    coVerify(exactly = 0) { userService.updateUserSubscription(any(), any()) }
                }
        }

        "ignore as no user id metadata received" {
            val event = Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "invoice.paid"
                data = EventData().apply {
                    setObject(Invoice().apply {
                        `object` = "invoice"
                        subscription = "stripeSubscription1"
                        paymentIntent = "stripePaymentIntent1"
                        lines = InvoiceLineItemCollection().apply {
                            data = listOf(
                                InvoiceLineItem().apply {
                                    period = InvoiceLineItemPeriod().apply {
                                        start = Instant.now().epochSecond
                                        end = Instant.now().plusSeconds(120).epochSecond
                                    }
                                    metadata = mapOf(
                                        StripeSubscriptionMeta.SUBSCRIPTION_PLAN.addValue(UserSubscriptionPlan.PRO),
                                        StripeSubscriptionMeta.REFUND_DURATION.addValue(Duration.ofDays(3)),
                                    )
                                }
                            )
                        }
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                }
            }

            val userService = mockk<UserService>()

            ApplicationContextRunner()
                .withBean(UserService::class.java, { userService })
                .withBean(UpdateUserSubscriptionStripeWebhookEventHandler::class.java)
                .run { context ->
                    context.publishEvent(event)

                    coVerify(exactly = 0) { userService.updateUserSubscription(any(), any()) }
                }
        }
    }
})