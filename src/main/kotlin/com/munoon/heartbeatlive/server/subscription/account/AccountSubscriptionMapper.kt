package com.munoon.heartbeatlive.server.subscription.account

import com.munoon.heartbeatlive.server.common.GraphqlMoney
import com.munoon.heartbeatlive.server.config.properties.SubscriptionProperties
import com.munoon.heartbeatlive.server.subscription.account.model.GraphqlPaymentProviderName
import com.munoon.heartbeatlive.server.subscription.account.model.GraphqlStripePaymentProvider
import com.munoon.heartbeatlive.server.subscription.account.model.GraphqlSubscriptionPlan
import com.munoon.heartbeatlive.server.subscription.account.model.PaymentProviderInfo
import com.munoon.heartbeatlive.server.subscription.account.model.StripePaymentProviderInfo
import com.munoon.heartbeatlive.server.subscription.account.provider.PaymentProviderName
import com.munoon.heartbeatlive.server.user.User
import java.util.*

object AccountSubscriptionMapper {
    fun PaymentProviderInfo.asGraphqlProviderInfo() = when (this) {
        is StripePaymentProviderInfo -> GraphqlStripePaymentProvider(publicKey)
    }

    fun SubscriptionProperties.Subscription.asGraphqlSubscription(
        plan: UserSubscriptionPlan,
        locale: Locale?
    ) = GraphqlSubscriptionPlan(
        codeName = plan.name.lowercase(),
        displayName = displayName.getTranslation(locale) ?: plan.name.lowercase(),
        prices = prices.map { GraphqlSubscriptionPlan.Price(
            id = it.getId(plan),
            price = GraphqlMoney(it.price, it.currency),
            oldPrice = it.oldPrice?.let { oldPrice -> GraphqlMoney(oldPrice, it.currency) },
            duration = it.duration,
            refundDuration = it.refundDuration
        ) },
        limits = GraphqlSubscriptionPlan.Limits(
            maxSharingCodesLimit = limits.maxSharingCodesLimit,
            maxSubscribersLimit = limits.maxSubscribersLimit,
            maxSubscriptionsLimit = limits.maxSubscriptionsLimit
        ),
        info = GraphqlSubscriptionPlan.Info(
            descriptionItems = info.descriptionItems.getTranslation(locale) ?: emptyList()
        )
    )

    fun User.asSubscriptionJwt() = subscription?.let { JwtUserSubscription(
        plan = it.plan,
        expirationTime = it.expiresAt
    ) } ?: JwtUserSubscription.DEFAULT

    fun GraphqlPaymentProviderName.asPaymentProviderName() = when (this) {
        GraphqlPaymentProviderName.STRIPE -> PaymentProviderName.STRIPE
    }

    private fun <T> Map<Locale, T>.getTranslation(locale: Locale?): T? {
        if (locale != null) {
            get(Locale(locale.language))?.let { return it }
        }
        return get(Locale.ROOT) ?: get(Locale.ENGLISH)
    }
}