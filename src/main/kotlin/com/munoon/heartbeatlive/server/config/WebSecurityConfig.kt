package com.munoon.heartbeatlive.server.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.munoon.heartbeatlive.server.auth.jwt.CustomJwtAuthenticationToken
import com.nimbusds.jose.Header
import com.nimbusds.jose.util.Base64URL
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.config.web.server.invoke
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimNames
import org.springframework.security.oauth2.jwt.JwtValidationException
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher
import org.springframework.web.filter.reactive.ServerWebExchangeContextFilter
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.time.Instant

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class WebSecurityConfig {
    @Bean
    fun securityWebFilterChain(
        http: ServerHttpSecurity,
        jwtDecoder: ReactiveJwtDecoder
    ): SecurityWebFilterChain {
        return http {
            csrf { disable() }

            oauth2ResourceServer {
                jwt {
                    this.jwtAuthenticationConverter = Converter { CustomJwtAuthenticationToken(it).toMono() }
                    this.jwtDecoder = jwtDecoder
                }
            }

            authorizeExchange {
                authorize(graphqlEndpointWebExchangeMatcher(), permitAll)
                authorize(graphqlWebsocketEndpointWebExchangeMatcher(), permitAll)
                authorize("/api/stripe/**", permitAll)
                authorize("/.well-known/apple-app-site-association", permitAll)
                authorize(anyExchange, denyAll)
            }
        }
    }

    @Bean
    fun graphqlEndpointWebExchangeMatcher(): ServerWebExchangeMatcher {
        return PathPatternParserServerWebExchangeMatcher("/graphql", HttpMethod.POST)
    }

    @Bean
    fun graphqlWebsocketEndpointWebExchangeMatcher(
        graphqlProperties: GraphQlProperties? = null
    ): ServerWebExchangeMatcher {
        return AndServerWebExchangeMatcher(
            PathPatternParserServerWebExchangeMatcher(graphqlProperties!!.websocket.path, HttpMethod.GET),
            {
                if (it.request.headers.upgrade.equals("websocket", ignoreCase = true))
                    ServerWebExchangeMatcher.MatchResult.match()
                else ServerWebExchangeMatcher.MatchResult.notMatch()
            }
        )
    }

    @Bean
    @Profile("!test")
    fun firebaseAuth(): FirebaseAuth {
        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.getApplicationDefault())
            .build()

        val firebaseApp = FirebaseApp.initializeApp(options)
        return FirebaseAuth.getInstance(firebaseApp)
    }

    @Bean
    fun jwtDecoder(firebaseAuth: FirebaseAuth): ReactiveJwtDecoder =
        object : ReactiveJwtDecoder {
            private val defaultOAuth2Error =
                OAuth2Error("JWT token is not valid. Please, use ID token generated by Firebase.")

            override fun decode(token: String?): Mono<Jwt> {
                token ?: return Mono.empty()

                val firebaseToken = try {
                    firebaseAuth.verifyIdToken(token)
                } catch (_: FirebaseAuthException) {
                    throw JwtValidationException("JWT token is not valid!", listOf(defaultOAuth2Error))
                }

                val tokenHeader = Base64URL(token.split(".", limit = 2)[0])

                return Jwt.withTokenValue(token)
                    .claims { it.putAll(firebaseToken.claims) }
                    .headers { it.putAll(Header.parse(tokenHeader).toJSONObject()) }
                    .subject(firebaseToken.uid)
                    .claim(JwtClaimNames.IAT, Instant.ofEpochSecond(firebaseToken.claims["iat"] as Long))
                    .claim(JwtClaimNames.EXP, Instant.ofEpochSecond(firebaseToken.claims["exp"] as Long))
                    .claim("auth_time", Instant.ofEpochSecond(firebaseToken.claims["auth_time"] as Long))
                    .build()
                    .toMono()
            }
        }

    @Bean
    fun serverWebExchangeContextFilter() = ServerWebExchangeContextFilter()
}