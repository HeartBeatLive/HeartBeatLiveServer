package com.munoon.heartbeatlive.server.subscription.account.stripe.webhook

import com.google.gson.JsonObject
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeMetadata
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.UserNotFoundByIdException
import com.munoon.heartbeatlive.server.user.service.UserService
import com.stripe.Stripe
import com.stripe.model.Customer
import com.stripe.model.Event
import com.stripe.model.EventData
import com.stripe.model.Subscription
import com.stripe.net.ApiResource
import io.kotest.core.spec.style.FreeSpec
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.time.Duration
import java.time.Instant

class SubscriptionDeletedStripeWebhookEventHandlerTest : FreeSpec({
    "handleEvent" - {
        "successful" {
            val event = Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "customer.subscription.deleted"
                data = EventData().apply {
                    setObject(Subscription().apply {
                        `object` = "subscription"
                        id = "stripeSubscription1"
                        metadata = mapOf(
                            StripeMetadata.Subscription.USER_ID.addValue("user1")
                        )
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                }
            }

            val userService = mockk<UserService>() {
                val user = User(
                    id = "user1",
                    displayName = null,
                    email = null,
                    emailVerified = false,
                    subscription = User.Subscription(
                        plan = UserSubscriptionPlan.PRO,
                        expiresAt = Instant.now(),
                        startAt = Instant.now(),
                        details = User.Subscription.StripeSubscriptionDetails(
                            subscriptionId = "stripeSubscription1",
                            paymentIntentId = "stripePaymentIntent1"
                        ),
                        refundDuration = Duration.ofSeconds(30)
                    )
                )
                coEvery { getUserById(any()) } returns user
                coEvery { updateUserSubscription(any(), any()) } returns user
            }

            ApplicationContextRunner()
                .withBean(UserService::class.java, { userService })
                .withBean(SubscriptionDeletedStripeWebhookEventHandler::class.java)
                .run { context ->
                    context.publishEvent(event)

                    coVerify(exactly = 1) { userService.getUserById("user1") }
                    coVerify(exactly = 1) { userService.updateUserSubscription("user1", null) }
                }
        }

        "successful with deleted user" {
            val event = Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "customer.subscription.deleted"
                data = EventData().apply {
                    setObject(Subscription().apply {
                        `object` = "subscription"
                        id = "stripeSubscription1"
                        metadata = mapOf(
                            StripeMetadata.Subscription.USER_ID.addValue("user1")
                        )
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                }
            }

            val userService = mockk<UserService>() {
                coEvery { getUserById(any()) } throws UserNotFoundByIdException("user1")
            }

            ApplicationContextRunner()
                .withBean(UserService::class.java, { userService })
                .withBean(SubscriptionDeletedStripeWebhookEventHandler::class.java)
                .run { context ->
                    context.publishEvent(event)

                    coVerify(exactly = 1) { userService.getUserById("user1") }
                    coVerify(exactly = 0) { userService.updateUserSubscription(any(), any()) }
                }
        }

        "ignored as other type" {
            val event = Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "other_type"
                data = EventData().apply {
                    setObject(Subscription().apply {
                        `object` = "subscription"
                        id = "stripeSubscription1"
                        metadata = mapOf(
                            StripeMetadata.Subscription.USER_ID.addValue("user1")
                        )
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                }
            }

            val userService = mockk<UserService>()
            ApplicationContextRunner()
                .withBean(UserService::class.java, { userService })
                .withBean(SubscriptionDeletedStripeWebhookEventHandler::class.java)
                .run { context ->
                    context.publishEvent(event)

                    coVerify(exactly = 0) { userService.getUserById(any()) }
                    coVerify(exactly = 0) { userService.updateUserSubscription(any(), any()) }
                }
        }

        "ignored as no subscription info" {
            val event = Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "customer.subscription.deleted"
                data = EventData().apply {
                    setObject(Customer().apply {
                        `object` = "customer"
                        id = "stripeCustomer1"
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                }
            }

            val userService = mockk<UserService>()
            ApplicationContextRunner()
                .withBean(UserService::class.java, { userService })
                .withBean(SubscriptionDeletedStripeWebhookEventHandler::class.java)
                .run { context ->
                    context.publishEvent(event)

                    coVerify(exactly = 0) { userService.getUserById(any()) }
                    coVerify(exactly = 0) { userService.updateUserSubscription(any(), any()) }
                }
        }

        "ignored as no subscription id" {
            val event = Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "customer.subscription.deleted"
                data = EventData().apply {
                    setObject(Subscription().apply {
                        `object` = "subscription"
                        metadata = mapOf(
                            StripeMetadata.Subscription.USER_ID.addValue("user1")
                        )
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                }
            }

            val userService = mockk<UserService>()
            ApplicationContextRunner()
                .withBean(UserService::class.java, { userService })
                .withBean(SubscriptionDeletedStripeWebhookEventHandler::class.java)
                .run { context ->
                    context.publishEvent(event)

                    coVerify(exactly = 0) { userService.getUserById(any()) }
                    coVerify(exactly = 0) { userService.updateUserSubscription(any(), any()) }
                }
        }

        "ignored as no user id metadata" {
            val event = Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "customer.subscription.deleted"
                data = EventData().apply {
                    setObject(Subscription().apply {
                        `object` = "subscription"
                        id = "stripeSubscription1"
                        metadata = mapOf()
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                }
            }

            val userService = mockk<UserService>()
            ApplicationContextRunner()
                .withBean(UserService::class.java, { userService })
                .withBean(SubscriptionDeletedStripeWebhookEventHandler::class.java)
                .run { context ->
                    context.publishEvent(event)

                    coVerify(exactly = 0) { userService.getUserById(any()) }
                    coVerify(exactly = 0) { userService.updateUserSubscription(any(), any()) }
                }
        }

        "ignored as user currently have another subscription" {
            val event = Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "customer.subscription.deleted"
                data = EventData().apply {
                    setObject(Subscription().apply {
                        `object` = "subscription"
                        id = "stripeSubscription1"
                        metadata = mapOf(
                            StripeMetadata.Subscription.USER_ID.addValue("user1")
                        )
                    }.let { ApiResource.GSON.toJsonTree(it) as JsonObject })
                }
            }

            val userService = mockk<UserService>() {
                coEvery { getUserById(any()) } returns User(
                    id = "user1",
                    displayName = null,
                    email = null,
                    emailVerified = false,
                    subscription = User.Subscription(
                        plan = UserSubscriptionPlan.PRO,
                        expiresAt = Instant.now(),
                        startAt = Instant.now(),
                        details = User.Subscription.StripeSubscriptionDetails(
                            subscriptionId = "stripeSubscription2",
                            paymentIntentId = "stripePaymentIntent1"
                        ),
                        refundDuration = Duration.ofSeconds(30)
                    )
                )
            }

            ApplicationContextRunner()
                .withBean(UserService::class.java, { userService })
                .withBean(SubscriptionDeletedStripeWebhookEventHandler::class.java)
                .run { context ->
                    context.publishEvent(event)

                    coVerify(exactly = 1) { userService.getUserById("user1") }
                    coVerify(exactly = 0) { userService.updateUserSubscription(any(), any()) }
                }
        }
    }
})