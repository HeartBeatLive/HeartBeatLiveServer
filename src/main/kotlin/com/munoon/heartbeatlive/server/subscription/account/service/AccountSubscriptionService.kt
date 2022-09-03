package com.munoon.heartbeatlive.server.subscription.account.service

import com.munoon.heartbeatlive.server.subscription.account.AccountSubscriptionMapper.asPaymentProviderName
import com.munoon.heartbeatlive.server.subscription.account.AccountSubscriptionUtils.getActiveSubscriptionPlan
import com.munoon.heartbeatlive.server.subscription.account.PaymentProviderNotFoundException
import com.munoon.heartbeatlive.server.subscription.account.RefundPeriodEndException
import com.munoon.heartbeatlive.server.subscription.account.UserHaveNotActiveSubscriptionException
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import com.munoon.heartbeatlive.server.subscription.account.limit.AccountSubscriptionLimit
import com.munoon.heartbeatlive.server.subscription.account.model.GraphqlPaymentProviderName
import com.munoon.heartbeatlive.server.subscription.account.model.PaymentProviderInfo
import com.munoon.heartbeatlive.server.subscription.account.provider.PaymentProvider
import com.munoon.heartbeatlive.server.subscription.account.provider.PaymentProviderName
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.UserEvents
import com.munoon.heartbeatlive.server.user.service.UserService
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class AccountSubscriptionService(
    private val providers: List<PaymentProvider>,
    private val userService: UserService,
    private val limits: List<AccountSubscriptionLimit<*>>
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
            ?: throw IllegalArgumentException("Payment provider '$providerName' is not found!")

    @Async
    @EventListener
    fun handleUserUpdatedEvent(event: UserEvents.UserUpdatedEvent) {
        val oldSubscriptionPlan = event.oldUser.subscription?.plan ?: UserSubscriptionPlan.FREE
        val newSubscriptionPlan = event.newUser.subscription?.plan ?: UserSubscriptionPlan.FREE
        if (oldSubscriptionPlan == newSubscriptionPlan) {
            return
        }

        for (limit in limits) {
            try {
                runBlocking { limit.maintainALimit(event.newUser.id, newSubscriptionPlan) }
            } catch (e: Exception) {
                logger.error("Exception happened while maintaining a user '${event.newUser.id}' new limit", e)
            }
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(AccountSubscriptionService::class.java)

        fun User.Subscription.SubscriptionDetails.getPaymentProviderName() = when (this) {
            is User.Subscription.StripeSubscriptionDetails -> PaymentProviderName.STRIPE
        }
    }
}