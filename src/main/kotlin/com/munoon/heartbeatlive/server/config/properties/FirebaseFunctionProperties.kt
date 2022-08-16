package com.munoon.heartbeatlive.server.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("auth.firebase.function")
class FirebaseFunctionProperties {
    lateinit var token: String
}