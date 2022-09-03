package com.munoon.heartbeatlive.server.subscription.account.stripe.webhook

import com.munoon.heartbeatlive.server.email.SubscriptionInvoiceFailedToRefundedEmailMessage
import com.munoon.heartbeatlive.server.email.SubscriptionInvoiceSuccessfullyRefundedEmailMessage
import com.munoon.heartbeatlive.server.email.service.EmailService
import com.munoon.heartbeatlive.server.push.RefundFailedPushNotificationData
import com.munoon.heartbeatlive.server.push.service.PushNotificationService
import com.munoon.heartbeatlive.server.push.service.sendNotifications
import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeMetadata
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.UserUtils.getVerifiedEmailAddress
import com.munoon.heartbeatlive.server.user.service.UserService
import com.stripe.model.Charge
import com.stripe.model.Event
import com.stripe.model.Refund
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class RefundStripeWebhookEventHandler(
    private val emailService: EmailService,
    private val pushNotificationService: PushNotificationService,
    private val userService: UserService
) {
    private val logger = LoggerFactory.getLogger(RefundStripeWebhookEventHandler::class.java)

    @EventListener(condition = "#event.type == 'charge.refunded'")
    fun handleChargeRefundedEvent(event: Event) {
        val charge = event.dataObjectDeserializer.`object`?.get() as Charge
        if (charge.status != "succeeded") {
            logger.warn("Ignoring 'charge.refunded' event as charge status is not 'succeeded'")
            return
        }

        val refund = charge.refunds.data.firstOrNull()
        if (refund == null) {
            logger.warn("Ignoring 'charge.refunded' event as refund info found")
            return
        }

        refund.handleEvent("charge.refunded", "succeeded") { user ->
            emailService.send(SubscriptionInvoiceSuccessfullyRefundedEmailMessage(user.getVerifiedEmailAddress()))
        }
    }

    @EventListener(condition = "#event.type == 'charge.refund.updated'")
    fun handleChargeFailedToRefundEvent(event: Event) {
        val refund = event.dataObjectDeserializer.`object`?.get() as Refund
        refund.handleEvent("charge.refund.updated", "failed") { user ->
            pushNotificationService.sendNotifications(RefundFailedPushNotificationData(user.id))
            emailService.send(SubscriptionInvoiceFailedToRefundedEmailMessage(user.getVerifiedEmailAddress()))
        }
    }

    private fun Refund.handleEvent(eventType: String, expectStatus: String, finalLogic: suspend (User) -> Unit) {
        if (status != expectStatus) {
            logger.warn("Ignoring '$eventType' event as refund status is not '$expectStatus'")
            return
        }

        val userId = StripeMetadata.Refund.USER_ID.getValue(metadata)
        if (userId == null) {
            logger.warn("Ignoring '$eventType' event as no user id found in metadata")
            return
        }

        runBlocking {
            val user = userService.getUserById(userId)
            finalLogic(user)
        }
    }
}