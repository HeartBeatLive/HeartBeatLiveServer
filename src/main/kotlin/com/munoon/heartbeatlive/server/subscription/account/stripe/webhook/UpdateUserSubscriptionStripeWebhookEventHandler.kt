package com.munoon.heartbeatlive.server.subscription.account.stripe.webhook

import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeMetadata
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.service.UserService
import com.stripe.model.Event
import com.stripe.model.Invoice
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class UpdateUserSubscriptionStripeWebhookEventHandler(private val userService: UserService) {
    private val logger = LoggerFactory.getLogger(UpdateUserSubscriptionStripeWebhookEventHandler::class.java)

    @EventListener(condition = "#event.type == 'invoice.paid'")
    fun handleEvent(event: Event) {
        val invoice = event.dataObjectDeserializer?.`object`?.takeIf { it.isPresent }?.get() as? Invoice
        if (invoice == null) {
            logger.warn("Ignoring 'invoice.paid' stripe event as no invoice info received")
            return
        }

        if (invoice.subscription == null) {
            logger.warn("Ignoring 'invoice.paid' stripe event as no invoice subscription received")
            return
        }

        val lineItem = invoice.lines?.data?.firstOrNull()
        if (lineItem == null) {
            logger.warn("Ignoring 'invoice.paid' stripe event as no invoice line item received")
            return
        }

        val period = lineItem.period
        if (period == null) {
            logger.warn("Ignoring 'invoice.paid' stripe event as no period received")
            return
        }

        val subscriptionExpireAt = period.end?.let { Instant.ofEpochSecond(it) }
        if (subscriptionExpireAt == null) {
            logger.warn("Ignoring 'invoice.paid' stripe event as no end period received")
            return
        }

        val subscriptionStartedAt = period.start?.let { Instant.ofEpochSecond(it) }
        if (subscriptionStartedAt == null) {
            logger.warn("Ignoring 'invoice.paid' stripe event as no start period received")
            return
        }

        if (invoice.paymentIntent == null) {
            logger.warn("Ignoring 'invoice.paid' stripe event as no payment intent id received")
            return
        }

        val refundSeconds = StripeMetadata.Subscription.REFUND_DURATION.getValue(lineItem.metadata)
        if (refundSeconds == null) {
            logger.warn("Ignoring 'invoice.paid' stripe event as no 'refundSeconds' metadata received")
            return
        }

        val subscriptionPlan = StripeMetadata.Subscription.SUBSCRIPTION_PLAN.getValue(lineItem.metadata)
        if (subscriptionPlan == null) {
            logger.warn("Ignoring 'invoice.paid' stripe event as can't find subscription plan in metadata")
            return
        }

        val userId = StripeMetadata.Subscription.USER_ID.getValue(lineItem.metadata)
        if (userId == null) {
            logger.warn("Ignoring 'invoice.paid' stripe event as can't find user id in metadata")
            return
        }

        runBlocking {
            val subscription = User.Subscription(
                plan = subscriptionPlan,
                expiresAt = subscriptionExpireAt,
                startAt = subscriptionStartedAt,
                refundDuration = refundSeconds,
                details = User.Subscription.StripeSubscriptionDetails(
                    subscriptionId = invoice.subscription,
                    paymentIntentId = invoice.paymentIntent
                )
            )

            userService.updateUserSubscription(userId, subscription)
            logger.info("Updated subscription for user '$userId': $subscription")
        }
    }
}