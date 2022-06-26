package com.munoon.heartbeatlive.server.subscription.account.stripe.controller

import com.munoon.heartbeatlive.server.auth.utils.AuthUtils.authUserId
import com.munoon.heartbeatlive.server.config.properties.StripeConfigurationProperties
import com.munoon.heartbeatlive.server.config.properties.SubscriptionProperties
import com.munoon.heartbeatlive.server.subscription.account.AccountSubscriptionUtils.findSubscriptionPriceById
import com.munoon.heartbeatlive.server.subscription.account.PaymentProviderIsNotSupportedException
import com.munoon.heartbeatlive.server.subscription.account.SubscriptionPlanPriceIsNotFoundByIdException
import com.munoon.heartbeatlive.server.subscription.account.model.GraphqlPaymentProviderName
import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeAccountSubscriptionMapper.asGraphql
import com.munoon.heartbeatlive.server.subscription.account.stripe.model.GraphqlStripeSubscription
import com.munoon.heartbeatlive.server.subscription.account.stripe.service.StripeAccountSubscriptionService
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("isAuthenticated()")
class StripeAccountSubscriptionController(
    private val service: StripeAccountSubscriptionService,
    private val properties: StripeConfigurationProperties,
    private val subscriptionProperties: SubscriptionProperties
) {
    private val logger = LoggerFactory.getLogger(StripeAccountSubscriptionController::class.java)

    @MutationMapping
    suspend fun createStripeSubscription(@Argument subscriptionPlanPriceId: String): GraphqlStripeSubscription {
        logger.info("User '${authUserId()}' request a stripe subscription to pay " +
                "for '$subscriptionPlanPriceId' subscription price.")

        if (!properties.enabled) throw PaymentProviderIsNotSupportedException(GraphqlPaymentProviderName.STRIPE)

        val stripePriceId = subscriptionProperties.findSubscriptionPriceById(subscriptionPlanPriceId).stripePriceId
            ?: throw SubscriptionPlanPriceIsNotFoundByIdException(subscriptionPlanPriceId)
        return service.createSubscription(stripePriceId, authUserId()).asGraphql()
    }
}