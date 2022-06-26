package com.munoon.heartbeatlive.server.config.properties

import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("payment.stripe")
class StripeConfigurationProperties {
    var enabled: Boolean = false
    lateinit var publicApiKey: String
    lateinit var privateApiKey: String
    lateinit var products: Map<UserSubscriptionPlan, String>
    var webhookEndpointSecret: String? = null
}