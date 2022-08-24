package com.munoon.heartbeatlive.server.subscription.account.stripe.webhook

import com.munoon.heartbeatlive.server.email.InvoiceFailedEmailMessage
import com.munoon.heartbeatlive.server.email.service.EmailService
import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeMetadata
import com.munoon.heartbeatlive.server.subscription.account.stripe.service.StripeAccountSubscriptionService
import com.munoon.heartbeatlive.server.user.UserUtils.getVerifiedEmailAddress
import com.munoon.heartbeatlive.server.user.service.UserService
import com.stripe.model.Event
import com.stripe.model.Subscription
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class FailedRecurringPaymentStripeWebhookEventHandler(
    private val userService: UserService,
    private val stripeAccountSubscriptionService: StripeAccountSubscriptionService,
    private val emailService: EmailService
) {
    private val logger = LoggerFactory.getLogger(FailedRecurringPaymentStripeWebhookEventHandler::class.java)

    @EventListener(condition = "#event.type == 'customer.subscription.updated'")
    fun handleEvent(event: Event) {
        val subscription = event.dataObjectDeserializer?.`object`?.takeIf { it.isPresent }?.get() as? Subscription
        if (subscription == null) {
            logger.warn("Ignoring 'customer.subscription.updated' stripe event as no subscription info received")
            return
        }

        if (event.data.previousAttributes?.get("status") != "active" || subscription.status != "past_due") {
            return
        }

        val userId = StripeMetadata.Subscription.USER_ID.getValue(subscription.metadata)
        if (userId == null) {
            logger.warn("Ignoring 'customer.subscription.updated' stripe event as no user id found in subscription metadata")
            return
        }

        if (subscription.latestInvoice == null) {
            logger.warn("Ignoring 'customer.subscription.updated' stripe event as no latest invoice id found in subscription info")
            return
        }

        runBlocking {
            val user = userService.updateUserSubscription(userId, null)
            stripeAccountSubscriptionService.saveFailedRecurringCharge(user.id, subscription.latestInvoice)
            emailService.send(InvoiceFailedEmailMessage(user.getVerifiedEmailAddress()))
        }
    }
}