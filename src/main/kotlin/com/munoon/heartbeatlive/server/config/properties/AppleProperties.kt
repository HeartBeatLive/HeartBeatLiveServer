package com.munoon.heartbeatlive.server.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("apple")
class AppleProperties {
    var appIds: Set<String> = emptySet()
}