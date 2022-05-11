package com.munoon.heartbeatlive.server.config.properties

import com.munoon.heartbeatlive.server.subscription.UserSubscriptionPlan
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.util.*

@Component
@ConfigurationProperties("app")
class SubscriptionProperties {
    var subscription: Map<UserSubscriptionPlan, SubscriptionSettings> = EnumMap(UserSubscriptionPlan::class.java)

    class SubscriptionSettings {
        var maxSharingCodesLimit: Int = 100
    }

    operator fun get(subscriptionPlan: UserSubscriptionPlan): SubscriptionSettings {
        return subscription[subscriptionPlan] ?: SubscriptionSettings()
    }
}