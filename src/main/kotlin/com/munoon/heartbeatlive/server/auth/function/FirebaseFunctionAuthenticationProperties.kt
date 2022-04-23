package com.munoon.heartbeatlive.server.auth.function

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("auth.firebase.function")
class FirebaseFunctionAuthenticationProperties {
    lateinit var token: String
}
