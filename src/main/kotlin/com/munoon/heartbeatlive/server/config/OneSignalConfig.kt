package com.munoon.heartbeatlive.server.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class OneSignalConfig {
    @Bean
    fun webClient() = WebClient.create()
}