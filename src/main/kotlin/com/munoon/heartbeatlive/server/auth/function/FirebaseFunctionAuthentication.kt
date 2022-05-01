package com.munoon.heartbeatlive.server.auth.function

import com.munoon.heartbeatlive.server.config.properties.FirebaseAuthenticationProperties
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.http.HttpHeaders
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component
import org.springframework.web.filter.reactive.ServerWebExchangeContextFilter
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class FirebaseFunctionAuthentication(private val properties: FirebaseAuthenticationProperties) {
    suspend fun isFirebaseFunction(): Boolean {
        val exchange = getExchange() ?: return false
        val token = exchange.request.headers[HttpHeaders.AUTHORIZATION]?.firstOrNull() ?: return false
        return properties.function.token == token
    }

    suspend fun checkIsFirebaseFunction() {
        if (!isFirebaseFunction()) {
            throw AccessDeniedException("This method is available only for firebase function!")
        }
    }

    private companion object {
        suspend fun getExchange() = Mono.deferContextual { context ->
            context.getOrEmpty<ServerWebExchange>(ServerWebExchangeContextFilter.EXCHANGE_CONTEXT_ATTRIBUTE)
                .map { Mono.just(it) }
                .orElseGet { Mono.empty() }
        }.awaitSingleOrNull()
    }
}