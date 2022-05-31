package com.munoon.heartbeatlive.server.config.websocket

import com.munoon.heartbeatlive.server.auth.jwt.CustomJwtAuthenticationToken
import org.springframework.graphql.server.WebGraphQlInterceptor
import org.springframework.graphql.server.WebGraphQlRequest
import org.springframework.graphql.server.WebGraphQlResponse
import org.springframework.graphql.server.WebSocketGraphQlInterceptor
import org.springframework.graphql.server.WebSocketGraphQlRequest
import org.springframework.graphql.server.WebSocketSessionInfo
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class WebSocketAuthenticationInterceptor(
    private val jwtDecoder: ReactiveJwtDecoder,
) : WebSocketGraphQlInterceptor {
    private companion object {
        const val TOKEN_KEY_NAME = "token"
        const val TOKEN_PREFIX = "Bearer "
        private val AUTHENTICATION_SESSION_ATTRIBUTE_KEY =
            WebSocketAuthenticationInterceptor::class.qualifiedName + ".authentication"

        fun WebSocketSessionInfo.getAuthentication(): CustomJwtAuthenticationToken? =
            attributes[AUTHENTICATION_SESSION_ATTRIBUTE_KEY] as? CustomJwtAuthenticationToken

        fun WebSocketSessionInfo.setAuthentication(authentication: CustomJwtAuthenticationToken) {
            attributes[AUTHENTICATION_SESSION_ATTRIBUTE_KEY] = authentication
        }
    }

    override fun intercept(request: WebGraphQlRequest, chain: WebGraphQlInterceptor.Chain): Mono<WebGraphQlResponse> {
        val authentication = (request as? WebSocketGraphQlRequest)?.sessionInfo?.getAuthentication()
            ?: return chain.next(request)

        return chain.next(request)
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
    }

    override fun handleConnectionInitialization(
        sessionInfo: WebSocketSessionInfo,
        connectionInitPayload: MutableMap<String, Any>,
    ): Mono<Any> {
        val jwtToken = (connectionInitPayload[TOKEN_KEY_NAME] as? String)
            ?.takeIf { it.startsWith(TOKEN_PREFIX, ignoreCase = true) }
            ?.substring(TOKEN_PREFIX.length)
            ?: return Mono.empty()

        return jwtDecoder.decode(jwtToken)
            .map { CustomJwtAuthenticationToken(it) }
            .doOnNext { sessionInfo.setAuthentication(it) }
            .flatMap { Mono.empty() }
    }
}