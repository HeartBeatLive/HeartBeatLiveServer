package com.munoon.heartbeatlive.server.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("payment.stripe")
class StripeConfigurationProperties {
    var enabled: Boolean = false
    lateinit var publicApiKey: String
    lateinit var privateApiKey: String
    var webhookEndpointSecret: String? = null
}