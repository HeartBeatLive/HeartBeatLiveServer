package com.munoon.heartbeatlive.server.subscription.account.stripe.webhook

import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeMetadata
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.UserNotFoundByIdException
import com.munoon.heartbeatlive.server.user.service.UserService
import com.stripe.model.Event
import com.stripe.model.Subscription
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class SubscriptionDeletedStripeWebhookEventHandler(private val userService: UserService) {
    private val logger = LoggerFactory.getLogger(SubscriptionDeletedStripeWebhookEventHandler::class.java)

    @EventListener(condition = "#event.type == 'customer.subscription.deleted'")
    fun handleEvent(event: Event) {
        val subscription = event.dataObjectDeserializer?.`object`?.takeIf { it.isPresent }?.get() as? Subscription
        if (subscription == null) {
            logger.info("Ignoring 'customer.subscription.deleted' stripe event because no subscription has found")
            return
        }

        val subscriptionId = subscription.id
        if (subscriptionId == null) {
            logger.info("Ignoring 'customer.subscription.deleted' stripe event because no subscription id has found")
            return
        }

        val userId = StripeMetadata.Subscription.USER_ID.getValue(subscription.metadata)
        if (userId == null) {
            logger.info("Ignoring 'customer.subscription.deleted' stripe event " +
                    "because no user id in metadata has found")
            return
        }

        runBlocking {
            val user = try {
                userService.getUserById(userId)
            } catch (_: UserNotFoundByIdException) {
                return@runBlocking
            }
            val subscriptionDetails = user.subscription?.details
            if (subscriptionDetails is User.Subscription.StripeSubscriptionDetails
                    && subscriptionDetails.subscriptionId == subscriptionId) {
                userService.updateUserSubscription(userId, null)
                logger.info("Removed stripe subscription '$subscriptionId' of user '$userId' " +
                        "as received stripe 'customer.subscription.deleted' event")
            }
        }
    }
}