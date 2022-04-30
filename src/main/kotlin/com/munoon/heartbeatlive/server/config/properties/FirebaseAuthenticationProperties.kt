package com.munoon.heartbeatlive.server.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("auth.firebase")
class FirebaseAuthenticationProperties {
    lateinit var serviceAccountFile: Resource
    lateinit var function: FunctionProperties

    @ConstructorBinding
    data class FunctionProperties(val token: String)
}