package com.munoon.heartbeatlive.server.subscription.account.service

import com.munoon.heartbeatlive.server.subscription.account.AccountSubscriptionMapper.asPaymentProviderName
import com.munoon.heartbeatlive.server.subscription.account.AccountSubscriptionUtils.getActiveSubscriptionPlan
import com.munoon.heartbeatlive.server.subscription.account.PaymentProviderNotFoundException
import com.munoon.heartbeatlive.server.subscription.account.RefundPeriodEndException
import com.munoon.heartbeatlive.server.subscription.account.UserHaveNotActiveSubscriptionException
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import com.munoon.heartbeatlive.server.subscription.account.model.GraphqlPaymentProviderName
import com.munoon.heartbeatlive.server.subscription.account.model.PaymentProviderInfo
import com.munoon.heartbeatlive.server.subscription.account.provider.PaymentProvider
import com.munoon.heartbeatlive.server.subscription.account.provider.PaymentProviderName
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.service.UserService
import org.springframework.stereotype.Service
import java.time.Instant

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
        getPaymentProvider(subscriptionDetails.getPaymentProviderName())
            .stopRenewingSubscription(user, subscriptionDetails)
    }

    suspend fun requestARefund(userId: String) {
        val user = userService.getUserById(userId)
        if (user.getActiveSubscriptionPlan() == UserSubscriptionPlan.FREE) {
            throw UserHaveNotActiveSubscriptionException()
        }

        val subscription = user.subscription!!
        if (subscription.startAt + subscription.refundDuration <= Instant.now()) {
            throw RefundPeriodEndException()
        }

        getPaymentProvider(subscription.details.getPaymentProviderName()).makeARefund(user)
        userService.updateUserSubscription(userId, null)
    }

    private fun getPaymentProvider(providerName: PaymentProviderName) =
        providers.find { it.providerName == providerName }
            ?: throw RuntimeException("Payment provider '$providerName' is not found!")

    private companion object {
        fun User.Subscription.SubscriptionDetails.getPaymentProviderName() = when (this) {
            is User.Subscription.StripeSubscriptionDetails -> PaymentProviderName.STRIPE
        }
    }
}