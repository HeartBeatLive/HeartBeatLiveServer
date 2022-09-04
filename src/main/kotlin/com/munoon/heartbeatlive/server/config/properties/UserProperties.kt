package com.munoon.heartbeatlive.server.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.time.Duration

@Component
@ConfigurationProperties("app.user")
class UserProperties {
    var resetPasswordRequestTimeout: Duration = Duration.ofMinutes(1)
}