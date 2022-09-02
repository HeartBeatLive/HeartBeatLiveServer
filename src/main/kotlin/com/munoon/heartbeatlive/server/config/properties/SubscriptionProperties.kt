package com.munoon.heartbeatlive.server.config.properties

import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Duration
import java.util.*

@Component
@ConfigurationProperties("app")
class SubscriptionProperties {
    var subscription: Map<UserSubscriptionPlan, Subscription> = EnumMap(UserSubscriptionPlan::class.java)

    class Subscription {
        var displayName: Map<Locale, String> = emptyMap()
        var prices: List<SubscriptionPrice> = emptyList()
        var limits = SubscriptionLimits()
        var info: SubscriptionInfo = SubscriptionInfo()
    }

    class SubscriptionLimits {
        var maxSharingCodesLimit: Int = 100
        var maxSubscribersLimit: Int = 10
        var maxSubscriptionsLimit: Int = 15
        var receiveHeartRateMatchNotification: Boolean = false
    }

    class SubscriptionInfo {
        var descriptionItems: Map<Locale, List<String>> = emptyMap()
    }

    class SubscriptionPrice {
        lateinit var price: BigDecimal
        lateinit var currency: String
        lateinit var duration: Duration
        var oldPrice: BigDecimal? = null
        var stripePriceId: String? = null
        var refundDuration: Duration = Duration.ofDays(3)

        fun getId(subscriptionPlan: UserSubscriptionPlan): String {
            val hash = Objects.hash(subscriptionPlan.name, price, currency, duration)
                .let { if (it < 0) it * -1 else it }
                .toString()
            return "subpr_$hash"
        }
    }

    operator fun get(subscriptionPlan: UserSubscriptionPlan): Subscription {
        return subscription[subscriptionPlan] ?: Subscription()
    }
}