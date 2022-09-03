package com.munoon.heartbeatlive.server.subscription.account.provider

import com.munoon.heartbeatlive.server.config.properties.StripeConfigurationProperties
import com.munoon.heartbeatlive.server.subscription.account.model.PaymentProviderInfo
import com.munoon.heartbeatlive.server.subscription.account.model.StripePaymentProviderInfo
import com.munoon.heartbeatlive.server.subscription.account.stripe.service.StripeAccountSubscriptionService
import com.munoon.heartbeatlive.server.user.User
import com.stripe.param.RefundCreateParams
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty("payment.stripe.enabled")
class StripePaymentProvider(
    private val properties: StripeConfigurationProperties,
    private val service: StripeAccountSubscriptionService
) : PaymentProvider {
    override val info: PaymentProviderInfo
        get() = StripePaymentProviderInfo(publicKey = properties.publicApiKey)

    override val providerName = PaymentProviderName.STRIPE

    override suspend fun stopRenewingSubscription(user: User, details: User.Subscription.SubscriptionDetails) {
        val subscriptionId = (details as User.Subscription.StripeSubscriptionDetails).subscriptionId
        service.cancelUserSubscription(subscriptionId)
    }

    override suspend fun makeARefund(user: User) {
        val subscriptionDetails = user.subscription!!.details as User.Subscription.StripeSubscriptionDetails
        service.makeARefund(
            userId = user.id,
            subscriptionId = subscriptionDetails.subscriptionId,
            paymentIntentId = subscriptionDetails.paymentIntentId,
            reason = RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER
        )
    }
}