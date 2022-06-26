package com.munoon.heartbeatlive.server.subscription.account.service

import com.munoon.heartbeatlive.server.subscription.account.AccountSubscriptionMapper.asPaymentProviderName
import com.munoon.heartbeatlive.server.subscription.account.AccountSubscriptionUtils.getActiveSubscriptionPlan
import com.munoon.heartbeatlive.server.subscription.account.PaymentProviderNotFoundException
import com.munoon.heartbeatlive.server.subscription.account.UserHaveNotActiveSubscriptionException
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import com.munoon.heartbeatlive.server.subscription.account.model.GraphqlPaymentProviderName
import com.munoon.heartbeatlive.server.subscription.account.model.PaymentProviderInfo
import com.munoon.heartbeatlive.server.subscription.account.provider.PaymentProvider
import com.munoon.heartbeatlive.server.subscription.account.provider.PaymentProviderName
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.service.UserService
import org.springframework.stereotype.Service

@Service
class AccountSubscriptionService(
    private val providers: List<PaymentProvider>,
    private val userService: UserService
) {
    fun getPaymentProviderInfo(supportedProviders: Set<GraphqlPaymentProviderName>): PaymentProviderInfo {
        val supportedProvidersNames = supportedProviders.map { it.asPaymentProviderName() }
        return providers.find { supportedProvidersNames.contains(it.providerName) }?.info
            ?: throw PaymentProviderNotFoundException()
    }

    suspend fun stopRenewingSubscription(userId: String) {
        val user = userService.getUserById(userId)
        if (user.getActiveSubscriptionPlan() == UserSubscriptionPlan.FREE) {
            throw UserHaveNotActiveSubscriptionException()
        }

        val subscriptionDetails = user.subscription!!.details
        val paymentProviderName = when (subscriptionDetails) {
            is User.Subscription.StripeSubscriptionDetails -> PaymentProviderName.STRIPE
        }

        val paymentProvider = providers.find { it.providerName == paymentProviderName }
            ?: throw RuntimeException("Can't stop renewing user '$userId' subscription, " +
                    "because payment provider $paymentProviderName is not found!")

        paymentProvider.stopRenewingSubscription(user, subscriptionDetails)
    }
}