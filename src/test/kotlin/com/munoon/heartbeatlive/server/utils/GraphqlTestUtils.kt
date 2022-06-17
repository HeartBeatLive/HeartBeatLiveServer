package com.munoon.heartbeatlive.server.utils

import org.assertj.core.api.Assertions.assertThat
import org.springframework.core.ParameterizedTypeReference
import org.springframework.graphql.ResponseError
import org.springframework.graphql.client.SubscriptionErrorException
import org.springframework.graphql.execution.ErrorType
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.http.HttpHeaders
import reactor.test.StepVerifier

object GraphqlTestUtils {
    fun GraphQlTester.Response.satisfyNoErrors() = errors().satisfy { assertThat(it).isEmpty() }

    private fun satisfyErrorResponse(
        error: ResponseError,
        expectedErrorType: ErrorType,
        expectedCode: String? = null,
        expectedExtensions: Map<String, Any?> = emptyMap(),
        expectedPath: String? = null
    ) {
        val extensions = expectedExtensions
            .let { if (expectedCode != null) it.plus("code" to expectedCode) else it }
            .plus("classification" to expectedErrorType.name)
        assertThat(error.extensions).isEqualTo(extensions)
        assertThat(error.path).isEqualTo(expectedPath)
    }

    fun GraphQlTester.Errors.expectSingleError(
        errorType: ErrorType,
        code: String? = null,
        extensions: Map<String, Any?> = emptyMap(),
        path: String? = null
    ) = satisfy {
        assertThat(it).hasSize(1)
        satisfyErrorResponse(it.first(), errorType, code, extensions, path)
    }

    fun GraphQlTester.Errors.expectSingleUnauthenticatedError(path: String?) = expectSingleError(
        errorType = ErrorType.FORBIDDEN,
        code = "common.access_denied",
        path = path
    )

    fun GraphQlTester.Errors.expectSingleValidationError(path: String?, vararg properties: String) = expectSingleError(
        errorType = ErrorType.BAD_REQUEST,
        code = "common.validation",
        path = path,
        extensions = mapOf("invalidProperties" to properties.toList())
    )

    inline fun <reified T : Any> GraphQlTester.Path.isEqualsTo(expected: T) = entity(T::class.java).isEqualTo(expected)

    inline fun <reified T : Any> GraphQlTester.Path.assertList(asserter: (List<T>) -> Unit): GraphQlTester.Path {
        val list = entityList(object : ParameterizedTypeReference<T>() {}).get()
        asserter(list)
        return this
    }

    inline fun <reified T : Any> GraphQlTester.Path.isEqualsToListOf(vararg items: T) =
        assertList<T> { assertThat(it).usingRecursiveComparison().isEqualTo(items.toList()) }

    fun StepVerifier.LastStep.expectSingleGraphQLError(
        errorType: ErrorType,
        code: String? = null,
        extensions: Map<String, Any?> = emptyMap(),
        path: String? = null
    ) = expectErrorSatisfies {
        assertThat(it).isInstanceOf(SubscriptionErrorException::class.java)
        val errors = (it as SubscriptionErrorException).errors
        assertThat(errors).hasSize(1)
        satisfyErrorResponse(errors.first(), errorType, code, extensions, path)
    }

    fun HttpGraphQlTester.language(language: String) = mutate().header(HttpHeaders.ACCEPT_LANGUAGE, language).build()
}