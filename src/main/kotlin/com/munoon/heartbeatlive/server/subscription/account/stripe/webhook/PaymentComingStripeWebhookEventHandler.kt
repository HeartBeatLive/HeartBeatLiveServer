package com.munoon.heartbeatlive.server.subscription.account.stripe.webhook

import com.munoon.heartbeatlive.server.email.SubscriptionInvoiceComingEmailMessage
import com.munoon.heartbeatlive.server.email.service.EmailService
import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeMetadata
import com.munoon.heartbeatlive.server.user.UserUtils.getVerifiedEmailAddress
import com.munoon.heartbeatlive.server.user.service.UserService
import com.stripe.model.Event
import com.stripe.model.Invoice
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class PaymentComingStripeWebhookEventHandler(
    private val userService: UserService,
    private val emailService: EmailService
) {
    private val logger = LoggerFactory.getLogger(PaymentComingStripeWebhookEventHandler::class.java)

    @EventListener(condition = "#event.type == 'invoice.upcoming'")
    fun handleEvent(event: Event) {
        val invoice = event.dataObjectDeserializer.`object`.orElse(null) as? Invoice
        if (invoice == null) {
            logger.warn("Ignoring 'invoice.upcoming' event as no invoice info received")
            return
        }

        val lineItem = invoice.lines.data.firstOrNull()
        if (lineItem == null) {
            logger.warn("Ignoring 'invoice.upcoming' event as no invoice line item received")
            return
        }

        val userId = StripeMetadata.Subscription.USER_ID.getValue(lineItem.metadata)
        if (userId == null) {
            logger.warn("Ignoring 'invoice.upcoming' event as no user id found in invoice line item")
            return
        }

        runBlocking {
            val user = userService.getUserById(userId)
            emailService.send(SubscriptionInvoiceComingEmailMessage(user.getVerifiedEmailAddress()))
        }
    }
}