package com.munoon.heartbeatlive.server.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("app.sharing-code")
class UserSharingProperties {
    lateinit var sharingUrlTemplate: String
}