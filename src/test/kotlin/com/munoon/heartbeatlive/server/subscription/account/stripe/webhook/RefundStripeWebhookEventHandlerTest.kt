package com.munoon.heartbeatlive.server.subscription.account.stripe.webhook

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.munoon.heartbeatlive.server.email.SubscriptionInvoiceFailedToRefundedEmailMessage
import com.munoon.heartbeatlive.server.email.SubscriptionInvoiceSuccessfullyRefundedEmailMessage
import com.munoon.heartbeatlive.server.email.service.EmailService
import com.munoon.heartbeatlive.server.push.RefundFailedPushNotificationData
import com.munoon.heartbeatlive.server.push.service.PushNotificationService
import com.munoon.heartbeatlive.server.push.service.sendNotifications
import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeMetadata
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.service.UserService
import com.stripe.Stripe
import com.stripe.model.Event
import com.stripe.model.EventData
import io.kotest.core.spec.style.FreeSpec
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.springframework.boot.test.context.runner.ApplicationContextRunner

internal class RefundStripeWebhookEventHandlerTest : FreeSpec({
    "handleChargeRefundedEvent" - {
        fun ignoreTest(event: Event) {
            val userService = mockk<UserService>()
            val emailService = mockk<EmailService>()

            ApplicationContextRunner()
                .withBean(UserService::class.java,  { userService })
                .withBean(EmailService::class.java, { emailService })
                .withBean(PushNotificationService::class.java, { mockk() })
                .withBean(RefundStripeWebhookEventHandler::class.java)
                .run { context ->
                    context.publishEvent(event)

                    coVerify(exactly = 0) { userService.getUserById(any()) }
                    coVerify(exactly = 0) { emailService.send(any()) }
                }
        }

        "successful" {
            val expectedEmail = SubscriptionInvoiceSuccessfullyRefundedEmailMessage("test@example.com")

            val event = Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "charge.refunded"
                data = EventData().apply {
                    setObject(JsonObject().apply {
                        addProperty("object", "charge")
                        addProperty("status", "succeeded")
                        add("refunds", JsonObject().apply {
                            add("data", JsonArray().apply {
                                add(JsonObject().apply {
                                    addProperty("object", "refund")
                                    addProperty("status", "succeeded")
                                    add("metadata", JsonObject().apply {
                                        addProperty(StripeMetadata.Refund.USER_ID.key, "user1")
                                    })
                                })
                            })
                        })
                    })
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
                .withBean(PushNotificationService::class.java, { mockk() })
                .withBean(RefundStripeWebhookEventHandler::class.java)
                .run { context ->
                    context.publishEvent(event)

                    coVerify(exactly = 1) { userService.getUserById("user1") }
                    coVerify(exactly = 1) { emailService.send(expectedEmail) }
                }
        }

        "ignore as another type" {
            ignoreTest(Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "test_type"
                data = EventData().apply {
                    setObject(JsonObject().apply {
                        addProperty("object", "charge")
                        addProperty("status", "succeeded")
                        add("refunds", JsonObject().apply {
                            add("data", JsonArray().apply {
                                add(JsonObject().apply {
                                    addProperty("object", "refund")
                                    addProperty("status", "succeeded")
                                    add("metadata", JsonObject().apply {
                                        addProperty(StripeMetadata.Refund.USER_ID.key, "user1")
                                    })
                                })
                            })
                        })
                    })
                }
            })
        }

        "ignore as charge status is not succeeded" {
            ignoreTest(Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "charge.refunded"
                data = EventData().apply {
                    setObject(JsonObject().apply {
                        addProperty("object", "charge")
                        addProperty("status", "failed")
                        add("refunds", JsonObject().apply {
                            add("data", JsonArray().apply {
                                add(JsonObject().apply {
                                    addProperty("object", "refund")
                                    addProperty("status", "succeeded")
                                    add("metadata", JsonObject().apply {
                                        addProperty(StripeMetadata.Refund.USER_ID.key, "user1")
                                    })
                                })
                            })
                        })
                    })
                }
            })
        }

        "ignore as no refund info found" {
            ignoreTest(Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "charge.refunded"
                data = EventData().apply {
                    setObject(JsonObject().apply {
                        addProperty("object", "charge")
                        addProperty("status", "succeeded")
                        add("refunds", JsonObject().apply {
                            add("data", JsonArray())
                        })
                    })
                }
            })
        }

        "ignore as refund status is not succeeded" {
            ignoreTest(Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "charge.refunded"
                data = EventData().apply {
                    setObject(JsonObject().apply {
                        addProperty("object", "charge")
                        addProperty("status", "succeeded")
                        add("refunds", JsonObject().apply {
                            add("data", JsonArray().apply {
                                add(JsonObject().apply {
                                    addProperty("object", "refund")
                                    addProperty("status", "failed")
                                    add("metadata", JsonObject().apply {
                                        addProperty(StripeMetadata.Refund.USER_ID.key, "user1")
                                    })
                                })
                            })
                        })
                    })
                }
            })
        }

        "ignore as no user id found in refund metadata" {
            ignoreTest(Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "charge.refunded"
                data = EventData().apply {
                    setObject(JsonObject().apply {
                        addProperty("object", "charge")
                        addProperty("status", "succeeded")
                        add("refunds", JsonObject().apply {
                            add("data", JsonArray().apply {
                                add(JsonObject().apply {
                                    addProperty("object", "refund")
                                    addProperty("status", "succeeded")
                                    add("metadata", JsonObject())
                                })
                            })
                        })
                    })
                }
            })
        }
    }

    "handleChargeFailedToRefundEvent" - {
        fun ignoreTest(event: Event) {
            val userService = mockk<UserService>()
            val emailService = mockk<EmailService>()
            val pushNotificationService = mockk<PushNotificationService>()

            ApplicationContextRunner()
                .withBean(UserService::class.java,  { userService })
                .withBean(EmailService::class.java, { emailService })
                .withBean(PushNotificationService::class.java, { pushNotificationService })
                .withBean(RefundStripeWebhookEventHandler::class.java)
                .run { context ->
                    context.publishEvent(event)

                    coVerify(exactly = 0) { userService.getUserById(any()) }
                    coVerify(exactly = 0) { emailService.send(any()) }
                    coVerify(exactly = 0) { pushNotificationService.sendNotifications(any()) }
                }
        }

        "successful" {
            val expectedEmail = SubscriptionInvoiceFailedToRefundedEmailMessage("test@example.com")
            val expectedNotification = RefundFailedPushNotificationData("user1")

            val event = Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "charge.refund.updated"
                data = EventData().apply {
                    setObject(JsonObject().apply {
                        addProperty("object", "refund")
                        addProperty("status", "failed")
                        add("metadata", JsonObject().apply {
                            addProperty(StripeMetadata.Refund.USER_ID.key, "user1")
                        })
                    })
                }
            }

            val userService = mockk<UserService>() {
                coEvery { getUserById(any()) } returns User(id = "user1", displayName = null,
                    email = "test@example.com", emailVerified = false)
            }

            val emailService = mockk<EmailService>() {
                coEvery { send(any()) } returns Unit
            }

            val pushNotificationService = mockk<PushNotificationService>() {
                coEvery { sendNotifications(any()) } returns Unit
            }

            ApplicationContextRunner()
                .withBean(UserService::class.java,  { userService })
                .withBean(EmailService::class.java, { emailService })
                .withBean(PushNotificationService::class.java, { pushNotificationService })
                .withBean(RefundStripeWebhookEventHandler::class.java)
                .run { context ->
                    context.publishEvent(event)

                    coVerify(exactly = 1) { userService.getUserById("user1") }
                    coVerify(exactly = 1) { emailService.send(expectedEmail) }
                    coVerify(exactly = 1) { pushNotificationService.sendNotifications(expectedNotification) }
                }
        }

        "ignore as another type" {
            ignoreTest(Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "charge.refund.deleted"
                data = EventData().apply {
                    setObject(JsonObject().apply {
                        addProperty("object", "refund")
                        addProperty("status", "failed")
                        add("metadata", JsonObject().apply {
                            addProperty(StripeMetadata.Refund.USER_ID.key, "user1")
                        })
                    })
                }
            })
        }

        "ignore as refund status is not failed" {
            ignoreTest(Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "charge.refund.updated"
                data = EventData().apply {
                    setObject(JsonObject().apply {
                        addProperty("object", "refund")
                        addProperty("status", "succeeded")
                        add("metadata", JsonObject().apply {
                            addProperty(StripeMetadata.Refund.USER_ID.key, "user1")
                        })
                    })
                }
            })
        }

        "ignore as no user id found in refund metadata" {
            ignoreTest(Event().apply {
                apiVersion = Stripe.API_VERSION
                type = "charge.refund.updated"
                data = EventData().apply {
                    setObject(JsonObject().apply {
                        addProperty("object", "refund")
                        addProperty("status", "failed")
                        add("metadata", JsonObject())
                    })
                }
            })
        }
    }
})