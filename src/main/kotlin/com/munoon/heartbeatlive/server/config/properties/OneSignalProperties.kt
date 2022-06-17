package com.munoon.heartbeatlive.server.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("one-signal")
class OneSignalProperties {
    lateinit var restApiKey: String
    lateinit var appId: String
}