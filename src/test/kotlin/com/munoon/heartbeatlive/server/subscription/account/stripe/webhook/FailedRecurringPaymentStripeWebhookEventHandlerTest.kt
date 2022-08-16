package com.munoon.heartbeatlive.server.subscription.account.stripe.webhook

import com.google.gson.JsonObject
import com.munoon.heartbeatlive.server.email.InvoiceFailedEmailMessage
import com.munoon.heartbeatlive.server.email.service.EmailService
import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeMetadata
import com.munoon.heartbeatlive.server.subscription.account.stripe.service.StripeAccountSubscriptionService
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.service.UserService
import com.stripe.Stripe
import com.stripe.model.Event
import com.stripe.model.EventData
import com.stripe.model.Invoice
import com.stripe.model.Subscription
import com.stripe.net.ApiResource
import io.kotest.core.spec.style.FreeSpec
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.springframework.boot.test.context.runner.ApplicationContextRunner

internal class FailedRecurringPaymentStripeWebhookEventHandlerTest : FreeSpec({
    "handleEvent" - {
        "save failed recurring charge" {
            val event = Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "customer.subscription.updated"
                data = EventData().apply {
                    setObject(Subscription().apply {
                        `object` = "subscription"
                        status = "past_due"
                        metadata = mapOf(
                            StripeMetadata.Subscription.USER_ID.addValue("user1")
                        )
                        latestInvoice = "stripeInvoice1"
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                    previousAttributes = mapOf("status" to "active")
                }
            }

            val userService = mockk<UserService>() {
                coEvery { updateUserSubscription(any(), any()) } returns User(id = "user1", displayName = null,
                    email = "email@example.com", emailVerified = false)
            }

            val stripeAccountSubscriptionService = mockk<StripeAccountSubscriptionService>() {
                coEvery { saveFailedRecurringCharge(any(), any()) } returns Unit
            }

            val emailService = mockk<EmailService>() {
                coEvery { send(any()) } returns Unit
            }

            ApplicationContextRunner()
                .withBean(FailedRecurringPaymentStripeWebhookEventHandler::class.java)
                .withBean(UserService::class.java, { userService })
                .withBean(StripeAccountSubscriptionService::class.java, { stripeAccountSubscriptionService })
                .withBean(EmailService::class.java, { emailService })
                .run { context ->
                    context.publishEvent(event)

                    coVerify(exactly = 1) { userService.updateUserSubscription("user1", null) }
                    coVerify(exactly = 1) { stripeAccountSubscriptionService.saveFailedRecurringCharge(
                        userId = "user1",
                        stripeInvoiceId = "stripeInvoice1"
                    ) }
                    coVerify(exactly = 1) { emailService.send(InvoiceFailedEmailMessage(email = "email@example.com")) }
                }
        }

        fun testIgnore(event: Event) {
            val userService = mockk<UserService>()
            val stripeAccountSubscriptionService = mockk<StripeAccountSubscriptionService>()
            val emailService = mockk<EmailService>()

            ApplicationContextRunner()
                .withBean(FailedRecurringPaymentStripeWebhookEventHandler::class.java)
                .withBean(UserService::class.java, { userService })
                .withBean(StripeAccountSubscriptionService::class.java, { stripeAccountSubscriptionService })
                .withBean(EmailService::class.java, { emailService })
                .run { context ->
                    context.publishEvent(event)

                    coVerify(exactly = 0) { userService.updateUserSubscription(any(), any()) }
                    coVerify(exactly = 0) { stripeAccountSubscriptionService.saveFailedRecurringCharge(any(), any()) }
                    coVerify(exactly = 0) { emailService.send(any()) }
                }
        }

        "ignore as another event type" {
            testIgnore(Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "another"
                data = EventData().apply {
                    setObject(Subscription().apply {
                        `object` = "subscription"
                        status = "past_due"
                        metadata = mapOf(
                            StripeMetadata.Subscription.USER_ID.addValue("user1")
                        )
                        latestInvoice = "stripeInvoice1"
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                    previousAttributes = mapOf("status" to "active")
                }
            })
        }

        "ignore as no subscription info provided" {
            testIgnore(Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "customer.subscription.updated"
                data = EventData().apply {
                    setObject(Invoice().apply {
                        `object` = "invoice"
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                    previousAttributes = mapOf("status" to "active")
                }
            })
        }

        "ignore as previous status was not 'active'" {
            testIgnore(Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "customer.subscription.updated"
                data = EventData().apply {
                    setObject(Subscription().apply {
                        `object` = "subscription"
                        status = "past_due"
                        metadata = mapOf(
                            StripeMetadata.Subscription.USER_ID.addValue("user1")
                        )
                        latestInvoice = "stripeInvoice1"
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                }
            })
        }

        "ignore as new status is not 'past_due'" {
            testIgnore(Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "customer.subscription.updated"
                data = EventData().apply {
                    setObject(Subscription().apply {
                        `object` = "subscription"
                        status = "paid"
                        metadata = mapOf(
                            StripeMetadata.Subscription.USER_ID.addValue("user1")
                        )
                        latestInvoice = "stripeInvoice1"
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                    previousAttributes = mapOf("status" to "active")
                }
            })
        }

        "ignore as no user id found" {
            testIgnore(Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "customer.subscription.updated"
                data = EventData().apply {
                    setObject(Subscription().apply {
                        `object` = "subscription"
                        status = "past_due"
                        metadata = mapOf()
                        latestInvoice = "stripeInvoice1"
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                    previousAttributes = mapOf("status" to "active")
                }
            })
        }

        "ignore as no latest invoice id found" {
            testIgnore(Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "customer.subscription.updated"
                data = EventData().apply {
                    setObject(Subscription().apply {
                        `object` = "subscription"
                        status = "past_due"
                        metadata = mapOf(
                            StripeMetadata.Subscription.USER_ID.addValue("user1")
                        )
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                    previousAttributes = mapOf("status" to "active")
                }
            })
        }
    }
})