package com.munoon.heartbeatlive.server.subscription.account.stripe.webhook

import com.google.gson.JsonObject
import com.munoon.heartbeatlive.server.config.properties.StripeConfigurationProperties
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeCustomerNotFoundByIdException
import com.munoon.heartbeatlive.server.subscription.account.stripe.service.StripeAccountSubscriptionService
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
import com.stripe.model.Price
import com.stripe.net.ApiResource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.time.Instant

internal class UpdateUserSubscriptionStripeWebhookEventHandlerTest {
    @Test
    fun handleEvent() {
        val subscriptionEndTime = Instant.now().plusSeconds(120).epochSecond
        val expectedUserSubscription = User.Subscription(
            plan = UserSubscriptionPlan.PRO,
            expiresAt = Instant.ofEpochSecond(subscriptionEndTime)
        )

        val event = Event().apply {
            apiVersion = Stripe.API_VERSION
            type = "invoice.paid"
            data = EventData().apply {
                setObject(Invoice().apply {
                    `object` = "invoice"
                    customer = "stripeCustomer1"
                    lines = InvoiceLineItemCollection().apply {
                        data = listOf(
                            InvoiceLineItem().apply {
                                period = InvoiceLineItemPeriod().apply {
                                    end = subscriptionEndTime
                                }
                                price = Price().apply {
                                    product = "stripeProduct1"
                                }
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
        val service = mockk<StripeAccountSubscriptionService>() {
            coEvery { getUserIdByStripeCustomerId(any()) } returns "user1"
        }
        val properties = StripeConfigurationProperties().apply {
            products = mapOf(UserSubscriptionPlan.PRO to "stripeProduct1")
        }

        ApplicationContextRunner()
            .withBean(UserService::class.java, { userService })
            .withBean(StripeAccountSubscriptionService::class.java, { service })
            .withBean(StripeConfigurationProperties::class.java, { properties })
            .withBean(UpdateUserSubscriptionStripeWebhookEventHandler::class.java)
            .run { context ->
                context.publishEvent(event)

                coVerify(exactly = 1) { service.getUserIdByStripeCustomerId("stripeCustomer1") }
                coVerify(exactly = 1) { userService.updateUserSubscription("user1", expectedUserSubscription) }
            }
    }

    @Test
    fun `handleEvent - ignore as other type`() {
        val event = Event().apply {
            apiVersion = Stripe.API_VERSION
            type = "test_type"
            data = EventData().apply {
                setObject(Invoice().apply {
                    `object` = "invoice"
                    customer = "stripeCustomer1"
                    lines = InvoiceLineItemCollection().apply {
                        data = listOf(
                            InvoiceLineItem().apply {
                                period = InvoiceLineItemPeriod().apply {
                                    end = Instant.now().plusSeconds(120).epochSecond
                                }
                                price = Price().apply {
                                    product = "stripeProduct1"
                                }
                            }
                        )
                    }
                }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
            }
        }

        val userService = mockk<UserService>()
        val service = mockk<StripeAccountSubscriptionService>()
        val properties = StripeConfigurationProperties().apply {
            products = mapOf(UserSubscriptionPlan.PRO to "stripeProduct1")
        }

        ApplicationContextRunner()
            .withBean(UserService::class.java, { userService })
            .withBean(StripeAccountSubscriptionService::class.java, { service })
            .withBean(StripeConfigurationProperties::class.java, { properties })
            .withBean(UpdateUserSubscriptionStripeWebhookEventHandler::class.java)
            .run { context ->
                context.publishEvent(event)

                coVerify(exactly = 0) { service.getUserIdByStripeCustomerId(any()) }
                coVerify(exactly = 0) { userService.updateUserSubscription(any(), any()) }
            }
    }

    @Test
    fun `handleEvent - ignore as no invoice info received`() {
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
        val service = mockk<StripeAccountSubscriptionService>()
        val properties = StripeConfigurationProperties().apply {
            products = mapOf(UserSubscriptionPlan.PRO to "stripeProduct1")
        }

        ApplicationContextRunner()
            .withBean(UserService::class.java, { userService })
            .withBean(StripeAccountSubscriptionService::class.java, { service })
            .withBean(StripeConfigurationProperties::class.java, { properties })
            .withBean(UpdateUserSubscriptionStripeWebhookEventHandler::class.java)
            .run { context ->
                context.publishEvent(event)

                coVerify(exactly = 0) { service.getUserIdByStripeCustomerId(any()) }
                coVerify(exactly = 0) { userService.updateUserSubscription(any(), any()) }
            }
    }

    @Test
    fun `handleEvent - ignore as no invoice customer received`() {
        val event = Event().apply {
            apiVersion = Stripe.API_VERSION
            type = "invoice.paid"
            data = EventData().apply {
                setObject(Invoice().apply {
                    `object` = "invoice"
                    lines = InvoiceLineItemCollection().apply {
                        data = listOf(
                            InvoiceLineItem().apply {
                                period = InvoiceLineItemPeriod().apply {
                                    end = Instant.now().plusSeconds(120).epochSecond
                                }
                                price = Price().apply {
                                    product = "stripeProduct1"
                                }
                            }
                        )
                    }
                }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
            }
        }

        val userService = mockk<UserService>()
        val service = mockk<StripeAccountSubscriptionService>()
        val properties = StripeConfigurationProperties().apply {
            products = mapOf(UserSubscriptionPlan.PRO to "stripeProduct1")
        }

        ApplicationContextRunner()
            .withBean(UserService::class.java, { userService })
            .withBean(StripeAccountSubscriptionService::class.java, { service })
            .withBean(StripeConfigurationProperties::class.java, { properties })
            .withBean(UpdateUserSubscriptionStripeWebhookEventHandler::class.java)
            .run { context ->
                context.publishEvent(event)

                coVerify(exactly = 0) { service.getUserIdByStripeCustomerId(any()) }
                coVerify(exactly = 0) { userService.updateUserSubscription(any(), any()) }
            }
    }

    @Test
    fun `handleEvent - ignore as no invoice line item received`() {
        val event = Event().apply {
            apiVersion = Stripe.API_VERSION
            type = "invoice.paid"
            data = EventData().apply {
                setObject(Invoice().apply {
                    `object` = "invoice"
                    customer = "stripeCustomer1"
                    lines = InvoiceLineItemCollection()
                }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
            }
        }

        val userService = mockk<UserService>()
        val service = mockk<StripeAccountSubscriptionService>()
        val properties = StripeConfigurationProperties().apply {
            products = mapOf(UserSubscriptionPlan.PRO to "stripeProduct1")
        }

        ApplicationContextRunner()
            .withBean(UserService::class.java, { userService })
            .withBean(StripeAccountSubscriptionService::class.java, { service })
            .withBean(StripeConfigurationProperties::class.java, { properties })
            .withBean(UpdateUserSubscriptionStripeWebhookEventHandler::class.java)
            .run { context ->
                context.publishEvent(event)

                coVerify(exactly = 0) { service.getUserIdByStripeCustomerId(any()) }
                coVerify(exactly = 0) { userService.updateUserSubscription(any(), any()) }
            }
    }

    @Test
    fun `handleEvent - ignore as no end period received`() {
        val event = Event().apply {
            apiVersion = Stripe.API_VERSION
            type = "invoice.paid"
            data = EventData().apply {
                setObject(Invoice().apply {
                    `object` = "invoice"
                    customer = "stripeCustomer1"
                    lines = InvoiceLineItemCollection().apply {
                        data = listOf(
                            InvoiceLineItem().apply {
                                period = InvoiceLineItemPeriod()
                                price = Price().apply {
                                    product = "stripeProduct1"
                                }
                            }
                        )
                    }
                }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
            }
        }

        val userService = mockk<UserService>()
        val service = mockk<StripeAccountSubscriptionService>()
        val properties = StripeConfigurationProperties().apply {
            products = mapOf(UserSubscriptionPlan.PRO to "stripeProduct1")
        }

        ApplicationContextRunner()
            .withBean(UserService::class.java, { userService })
            .withBean(StripeAccountSubscriptionService::class.java, { service })
            .withBean(StripeConfigurationProperties::class.java, { properties })
            .withBean(UpdateUserSubscriptionStripeWebhookEventHandler::class.java)
            .run { context ->
                context.publishEvent(event)

                coVerify(exactly = 0) { service.getUserIdByStripeCustomerId(any()) }
                coVerify(exactly = 0) { userService.updateUserSubscription(any(), any()) }
            }
    }

    @Test
    fun `handleEvent - ignore as subscription plan unresolved`() {
        val event = Event().apply {
            apiVersion = Stripe.API_VERSION
            type = "invoice.paid"
            data = EventData().apply {
                setObject(Invoice().apply {
                    `object` = "invoice"
                    customer = "stripeCustomer1"
                    lines = InvoiceLineItemCollection().apply {
                        data = listOf(
                            InvoiceLineItem().apply {
                                period = InvoiceLineItemPeriod().apply {
                                    end = Instant.now().plusSeconds(120).epochSecond
                                }
                                price = Price().apply {
                                    product = "stripeProductX"
                                }
                            }
                        )
                    }
                }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
            }
        }

        val userService = mockk<UserService>()
        val service = mockk<StripeAccountSubscriptionService>()
        val properties = StripeConfigurationProperties().apply {
            products = mapOf(UserSubscriptionPlan.PRO to "stripeProduct1")
        }

        ApplicationContextRunner()
            .withBean(UserService::class.java, { userService })
            .withBean(StripeAccountSubscriptionService::class.java, { service })
            .withBean(StripeConfigurationProperties::class.java, { properties })
            .withBean(UpdateUserSubscriptionStripeWebhookEventHandler::class.java)
            .run { context ->
                context.publishEvent(event)

                coVerify(exactly = 0) { service.getUserIdByStripeCustomerId(any()) }
                coVerify(exactly = 0) { userService.updateUserSubscription(any(), any()) }
            }
    }

    @Test
    fun `handleEvent - ignore as stripe customer not found`() {
        val event = Event().apply {
            apiVersion = Stripe.API_VERSION
            type = "invoice.paid"
            data = EventData().apply {
                setObject(Invoice().apply {
                    `object` = "invoice"
                    customer = "stripeCustomer1"
                    lines = InvoiceLineItemCollection().apply {
                        data = listOf(
                            InvoiceLineItem().apply {
                                period = InvoiceLineItemPeriod().apply {
                                    end = Instant.now().plusSeconds(120).epochSecond
                                }
                                price = Price().apply {
                                    product = "stripeProduct1"
                                }
                            }
                        )
                    }
                }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
            }
        }

        val userService = mockk<UserService>()
        val service = mockk<StripeAccountSubscriptionService>() {
            coEvery { getUserIdByStripeCustomerId(any()) } throws StripeCustomerNotFoundByIdException("stripeCustomer1")
        }
        val properties = StripeConfigurationProperties().apply {
            products = mapOf(UserSubscriptionPlan.PRO to "stripeProduct1")
        }

        ApplicationContextRunner()
            .withBean(UserService::class.java, { userService })
            .withBean(StripeAccountSubscriptionService::class.java, { service })
            .withBean(StripeConfigurationProperties::class.java, { properties })
            .withBean(UpdateUserSubscriptionStripeWebhookEventHandler::class.java)
            .run { context ->
                context.publishEvent(event)

                coVerify(exactly = 1) { service.getUserIdByStripeCustomerId("stripeCustomer1") }
                coVerify(exactly = 0) { userService.updateUserSubscription(any(), any()) }
            }
    }
}