package com.munoon.heartbeatlive.server.auth.function

import com.munoon.heartbeatlive.server.AbstractTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.reactor.mono
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.filter.reactive.ServerWebExchangeContextFilter
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@SpringBootTest(properties = ["auth.firebase.function.token=${FirebaseFunctionAuthenticationTest.FIREBASE_FUNCTION_TOKEN}"])
internal class FirebaseFunctionAuthenticationTest : AbstractTest() {
    companion object {
        const val FIREBASE_FUNCTION_TOKEN = "super-secret-firebase-function-token"

        private fun <T> runWithExchangeInContext(requestHeaders: HttpHeaders, run: suspend () -> T): T? =
            Mono.just(1)
                .flatMap { mono { run() } }
                .contextWrite {
                    val exchange = mockk<ServerWebExchange> {
                        val httpRequest = mockk<ServerHttpRequest>()
                        every { request }.returns(httpRequest)
                        every { httpRequest.headers }.returns(requestHeaders)
                    }

                    it.put(ServerWebExchangeContextFilter.EXCHANGE_CONTEXT_ATTRIBUTE, exchange)
                }
                .block()
    }

    @Autowired
    private lateinit var firebaseFunctionAuthentication: FirebaseFunctionAuthentication

    @Test
    fun `isFirebaseFunction - true`() {
        val requestHeaders = HttpHeaders().apply { set(HttpHeaders.AUTHORIZATION, FIREBASE_FUNCTION_TOKEN) }
        val result = runWithExchangeInContext(requestHeaders) { firebaseFunctionAuthentication.isFirebaseFunction() }
        assertThat(result).isTrue
    }

    @Test
    fun `isFirebaseFunction - no header`() {
        val requestHeaders = HttpHeaders.EMPTY
        val result = runWithExchangeInContext(requestHeaders) { firebaseFunctionAuthentication.isFirebaseFunction() }
        assertThat(result).isFalse
    }

    @Test
    fun `isFirebaseFunction - incorrect header value`() {
        val requestHeaders = HttpHeaders().apply { set(HttpHeaders.AUTHORIZATION, "abc") }
        val result = runWithExchangeInContext(requestHeaders) { firebaseFunctionAuthentication.isFirebaseFunction() }
        assertThat(result).isFalse
    }

    @Test
    fun `checkIsFirebaseFunction - true`() {
        val requestHeaders = HttpHeaders().apply { set(HttpHeaders.AUTHORIZATION, FIREBASE_FUNCTION_TOKEN) }
        assertThatNoException()
            .isThrownBy { runWithExchangeInContext(requestHeaders) { firebaseFunctionAuthentication.checkIsFirebaseFunction() } }
    }

    @Test
    fun `checkIsFirebaseFunction - false`() {
        val requestHeaders = HttpHeaders().apply { set(HttpHeaders.AUTHORIZATION, "abc") }
        assertThatThrownBy { runWithExchangeInContext(requestHeaders) { firebaseFunctionAuthentication.checkIsFirebaseFunction() } }
            .isExactlyInstanceOf(AccessDeniedException::class.java)
    }
}