package com.munoon.heartbeatlive.server.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.time.Duration

@Component
@ConfigurationProperties("payment.stripe")
class StripeConfigurationProperties {
    var enabled: Boolean = false
    lateinit var publicApiKey: String
    lateinit var privateApiKey: String
    var webhookEndpointSecret: String? = null
    var paymentRequiresActionWindow: Duration = Duration.ofHours(23)
}