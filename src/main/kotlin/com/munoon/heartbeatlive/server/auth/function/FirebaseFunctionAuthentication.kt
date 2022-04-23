package com.munoon.heartbeatlive.server.auth.function

import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.http.HttpHeaders
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component
import org.springframework.web.filter.reactive.ServerWebExchangeContextFilter
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class FirebaseFunctionAuthentication(private val properties: FirebaseFunctionAuthenticationProperties) {
    suspend fun isFirebaseFunction(): Boolean {
        val exchange = Mono.deferContextual { context ->
            context.getOrEmpty<ServerWebExchange>(ServerWebExchangeContextFilter.EXCHANGE_CONTEXT_ATTRIBUTE)
                .map { Mono.just(it) }
                .orElseGet { Mono.empty() }
        }.awaitSingleOrNull() ?: return false

        val token = exchange.request.headers[HttpHeaders.AUTHORIZATION]?.firstOrNull() ?: return false
        return properties.token == token
    }

    suspend fun checkIsFirebaseFunction() {
        if (!isFirebaseFunction()) {
            throw AccessDeniedException("This method is available only for firebase function!")
        }
    }
}