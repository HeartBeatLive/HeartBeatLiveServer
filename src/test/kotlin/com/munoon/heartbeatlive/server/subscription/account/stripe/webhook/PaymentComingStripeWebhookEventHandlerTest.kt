package com.munoon.heartbeatlive.server.subscription.account.stripe.webhook

import com.google.gson.JsonObject
import com.munoon.heartbeatlive.server.email.SubscriptionInvoiceComingEmailMessage
import com.munoon.heartbeatlive.server.email.service.EmailService
import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeMetadata
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.service.UserService
import com.stripe.Stripe
import com.stripe.model.Customer
import com.stripe.model.Event
import com.stripe.model.EventData
import com.stripe.model.Invoice
import com.stripe.model.InvoiceLineItem
import com.stripe.model.InvoiceLineItemCollection
import com.stripe.net.ApiResource
import io.kotest.core.spec.style.FreeSpec
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class PaymentComingStripeWebhookEventHandlerTest : FreeSpec({
    fun testIgnoreEvent(event: Event) {
        val userService = mockk<UserService>()
        val emailService = mockk<EmailService>()

        ApplicationContextRunner()
            .withBean(UserService::class.java,  { userService })
            .withBean(EmailService::class.java, { emailService })
            .withBean(PaymentComingStripeWebhookEventHandler::class.java)
            .run { context ->
                context.publishEvent(event)

                coVerify(exactly = 0) { userService.getUserById(any()) }
                coVerify(exactly = 0) { emailService.send(any()) }
            }
    }

    "handleEvent" - {
        "successful" {
            val expectedEmail = SubscriptionInvoiceComingEmailMessage("test@example.com")

            val event = Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "invoice.upcoming"
                data = EventData().apply {
                    setObject(Invoice().apply {
                        `object` = "invoice"
                        lines = InvoiceLineItemCollection().apply {
                            data = listOf(
                                InvoiceLineItem().apply {
                                    metadata = mapOf(
                                        StripeMetadata.Subscription.USER_ID.addValue("user1")
                                    )
                                }
                            )
                        }
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                }
            }

            val userService = mockk<UserService>() {
                coEvery { getUserById(any()) } returns User(id = "user1", displayName = null,
                    email = "test@example.com", emailVerified = false)
            }

            val emailService = mockk<EmailService>() {
                coEvery { send(any()) } returns Unit
            }

            ApplicationContextRunner()
                .withBean(UserService::class.java,  { userService })
                .withBean(EmailService::class.java, { emailService })
                .withBean(PaymentComingStripeWebhookEventHandler::class.java)
                .run { context ->
                    context.publishEvent(event)

                    coVerify(exactly = 1) { userService.getUserById("user1") }
                    coVerify(exactly = 1) { emailService.send(expectedEmail) }
                }
        }

        "ignore as another type" {
            testIgnoreEvent(Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "test_type"
                data = EventData().apply {
                    setObject(Invoice().apply {
                        `object` = "invoice"
                        lines = InvoiceLineItemCollection().apply {
                            data = listOf(
                                InvoiceLineItem().apply {
                                    metadata = mapOf(
                                        StripeMetadata.Subscription.USER_ID.addValue("user1")
                                    )
                                }
                            )
                        }
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                }
            })
        }

        "ignore as no invoice info" {
            testIgnoreEvent(Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "invoice.upcoming"
                data = EventData().apply {
                    setObject(Customer().apply {
                        `object` = "customer"
                        id = "customer1"
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                }
            })
        }

        "ignore as no line item info" {
            testIgnoreEvent(Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "invoice.upcoming"
                data = EventData().apply {
                    setObject(Invoice().apply {
                        `object` = "invoice"
                        lines = InvoiceLineItemCollection().apply {
                            data = listOf()
                        }
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                }
            })
        }

        "ignore as no user id metadata" {
            testIgnoreEvent(Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "invoice.upcoming"
                data = EventData().apply {
                    setObject(Invoice().apply {
                        `object` = "invoice"
                        lines = InvoiceLineItemCollection().apply {
                            data = listOf(
                                InvoiceLineItem().apply {
                                    metadata = mapOf()
                                }
                            )
                        }
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                }
            })
        }
    }
})