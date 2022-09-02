package com.munoon.heartbeatlive.server.subscription.account.stripe.webhook

import com.munoon.heartbeatlive.server.subscription.account.stripe.service.StripeAccountService
import com.stripe.model.Customer
import com.stripe.model.Event
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class DeleteCustomerStripeWebhookEventHandler(
    private val service: StripeAccountService
) {
    private val logger = LoggerFactory.getLogger(UpdateUserSubscriptionStripeWebhookEventHandler::class.java)

    @EventListener(condition = "#event.type == 'customer.deleted'")
    fun handleEvent(event: Event) {
        val customer = event.dataObjectDeserializer?.`object`?.takeIf { it.isPresent }?.get() as? Customer
        if (customer == null) {
            logger.warn("Ignoring 'customer.deleted' stripe event as no customer info received")
            return
        }

        runBlocking {
            service.deleteCustomerByStripeId(customer.id)
            logger.info("Deleted stripe customer with id '${customer.id}'")
        }
    }
}