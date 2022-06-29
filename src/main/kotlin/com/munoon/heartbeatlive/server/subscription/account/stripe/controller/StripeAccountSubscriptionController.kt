package com.munoon.heartbeatlive.server.subscription.account.stripe.controller

import com.munoon.heartbeatlive.server.auth.utils.AuthUtils.authUserId
import com.munoon.heartbeatlive.server.auth.utils.AuthUtils.authUserSubscription
import com.munoon.heartbeatlive.server.config.properties.StripeConfigurationProperties
import com.munoon.heartbeatlive.server.config.properties.SubscriptionProperties
import com.munoon.heartbeatlive.server.subscription.account.AccountSubscriptionUtils.findSubscriptionPriceById
import com.munoon.heartbeatlive.server.subscription.account.AccountSubscriptionUtils.getActiveSubscriptionPlan
import com.munoon.heartbeatlive.server.subscription.account.PaymentProviderIsNotSupportedException
import com.munoon.heartbeatlive.server.subscription.account.SubscriptionPlanPriceIsNotFoundByIdException
import com.munoon.heartbeatlive.server.subscription.account.UserAlreadyHaveActiveSubscriptionException
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import com.munoon.heartbeatlive.server.subscription.account.model.GraphqlPaymentProviderName
import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeAccountSubscriptionMapper.asGraphql
import com.munoon.heartbeatlive.server.subscription.account.stripe.model.GraphqlStripeSubscription
import com.munoon.heartbeatlive.server.subscription.account.stripe.service.StripeAccountSubscriptionService
import com.munoon.heartbeatlive.server.user.service.UserService
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
    private val subscriptionProperties: SubscriptionProperties,
    private val userService: UserService
) {
    private val logger = LoggerFactory.getLogger(StripeAccountSubscriptionController::class.java)

    @MutationMapping
    suspend fun createStripeSubscription(@Argument subscriptionPlanPriceId: String): GraphqlStripeSubscription {
        logger.info("User '${authUserId()}' request a stripe subscription to pay " +
                "for '$subscriptionPlanPriceId' subscription price.")

        if (!properties.enabled) throw PaymentProviderIsNotSupportedException(GraphqlPaymentProviderName.STRIPE)

        val (plan, price) = subscriptionProperties.findSubscriptionPriceById(subscriptionPlanPriceId)
        if (price.stripePriceId == null) {
            throw SubscriptionPlanPriceIsNotFoundByIdException(subscriptionPlanPriceId)
        }

        if (authUserSubscription() != UserSubscriptionPlan.FREE) {
            throw UserAlreadyHaveActiveSubscriptionException()
        }

        val user = userService.getUserById(authUserId())
        if (user.getActiveSubscriptionPlan() != UserSubscriptionPlan.FREE) {
            throw UserAlreadyHaveActiveSubscriptionException()
        }

        return service.createSubscription(plan, price, user).asGraphql()
    }
}