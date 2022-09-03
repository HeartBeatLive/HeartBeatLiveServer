package com.munoon.heartbeatlive.server.config

import kotlinx.serialization.json.Json
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.json.KotlinSerializationJsonDecoder
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig {
    @Bean
    fun webClient() = WebClient.builder()
        .exchangeStrategies(
            ExchangeStrategies.builder()
                .codecs { codecs ->
                    val json = Json {
                        ignoreUnknownKeys = true
                    }
                    codecs.defaultCodecs().kotlinSerializationJsonDecoder(KotlinSerializationJsonDecoder(json))
                }
                .build()
        )
        .build()
}