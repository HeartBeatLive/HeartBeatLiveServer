package com.munoon.heartbeatlive.server.subscription.account.stripe.webhook

import com.google.gson.JsonObject
import com.munoon.heartbeatlive.server.subscription.account.stripe.service.StripeAccountService
import com.stripe.Stripe
import com.stripe.model.Customer
import com.stripe.model.Event
import com.stripe.model.EventData
import com.stripe.model.Invoice
import com.stripe.net.ApiResource
import io.kotest.core.spec.style.FreeSpec
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.springframework.boot.test.context.runner.ApplicationContextRunner

internal class DeleteCustomerStripeWebhookEventHandlerTest : FreeSpec({
    "handleEvent" - {
        "delete customer" {
            val event = Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "customer.deleted"
                data = EventData().apply {
                    setObject(Customer().apply {
                        `object` = "customer"
                        id = "stripeCustomerId"
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                }
            }

            val service = mockk<StripeAccountService>() {
                coEvery { deleteCustomerByStripeId(any()) } returns Unit
            }

            ApplicationContextRunner()
                .withBean(StripeAccountService::class.java, { service })
                .withBean(DeleteCustomerStripeWebhookEventHandler::class.java)
                .run { context ->
                    context.publishEvent(event)

                    coVerify(exactly = 1) { service.deleteCustomerByStripeId("stripeCustomerId") }
                }
        }

        "ignore as customer not found" {
            val event = Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "customer.deleted"
                data = EventData().apply {
                    setObject(Invoice().apply {
                        `object` = "invoice"
                        id = "stripeCustomerId"
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                }
            }

            val service = mockk<StripeAccountService>()
            ApplicationContextRunner()
                .withBean(StripeAccountService::class.java, { service })
                .withBean(DeleteCustomerStripeWebhookEventHandler::class.java)
                .run { context ->
                    context.publishEvent(event)

                    coVerify(exactly = 0) { service.deleteCustomerByStripeId(any()) }
                }
        }

        "ignore as another event type" {
            val event = Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "invoice.paid"
                data = EventData().apply {
                    setObject(Customer().apply {
                        `object` = "customer"
                        id = "stripeCustomerId"
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                }
            }

            val service = mockk<StripeAccountService>()
            ApplicationContextRunner()
                .withBean(StripeAccountService::class.java, { service })
                .withBean(DeleteCustomerStripeWebhookEventHandler::class.java)
                .run { context ->
                    context.publishEvent(event)

                    coVerify(exactly = 0) { service.deleteCustomerByStripeId(any()) }
                }
        }
    }
})