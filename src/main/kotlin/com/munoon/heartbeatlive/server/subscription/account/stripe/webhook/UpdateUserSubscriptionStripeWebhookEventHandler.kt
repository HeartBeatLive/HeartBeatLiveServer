package com.munoon.heartbeatlive.server.subscription.account.stripe.webhook

import com.munoon.heartbeatlive.server.config.properties.StripeConfigurationProperties
import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeCustomerNotFoundByIdException
import com.munoon.heartbeatlive.server.subscription.account.stripe.service.StripeAccountSubscriptionService
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
class UpdateUserSubscriptionStripeWebhookEventHandler(
    private val userService: UserService,
    private val stripeAccountSubscriptionService: StripeAccountSubscriptionService,
    private val stripeConfigurationProperties: StripeConfigurationProperties
) {
    private val logger = LoggerFactory.getLogger(UpdateUserSubscriptionStripeWebhookEventHandler::class.java)

    @EventListener(condition = "#event.type == 'invoice.paid'")
    fun handleEvent(event: Event): Unit = runBlocking {
        val invoice = event.dataObjectDeserializer?.`object`?.takeIf { it.isPresent }?.get() as? Invoice
        if (invoice == null) {
            logger.warn("Ignoring 'invoice.paid' as no invoice info received")
            return@runBlocking
        }

        if (invoice.customer == null) {
            logger.warn("Ignoring 'invoice.paid' as no invoice customer received")
            return@runBlocking
        }

        val lineItem = invoice.lines?.data?.firstOrNull()
        if (lineItem == null) {
            logger.warn("Ignoring 'invoice.paid' as no invoice line item received")
            return@runBlocking
        }

        val subscriptionExpireAt = lineItem.period?.end?.let { Instant.ofEpochSecond(it) }
        if (subscriptionExpireAt == null) {
            logger.warn("Ignoring 'invoice.paid' as no end period received")
            return@runBlocking
        }

        val subscriptionPlan = stripeConfigurationProperties.products.entries
            .find { (_, productId) -> productId == lineItem.price?.product }
            ?.key
        if (subscriptionPlan == null) {
            logger.warn("Ignoring 'invoice.paid' as subscription plan unresolved")
            return@runBlocking
        }

        val userId = try {
            stripeAccountSubscriptionService.getUserIdByStripeCustomerId(invoice.customer)
        } catch (e: StripeCustomerNotFoundByIdException) {
            logger.warn("Ignoring 'invoice.paid' as stripe customer with id '${invoice.customer}' is not found")
            return@runBlocking
        }
        val subscription = User.Subscription(subscriptionPlan, subscriptionExpireAt)

        userService.updateUserSubscription(userId, subscription)
        logger.info("Updated subscription for user '$userId': $subscription")
    }
}