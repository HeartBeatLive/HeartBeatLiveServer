package com.munoon.heartbeatlive.server.utils

import org.assertj.core.api.Assertions.assertThat
import org.springframework.core.ParameterizedTypeReference
import org.springframework.graphql.execution.ErrorType
import org.springframework.graphql.test.tester.GraphQlTester

object GraphqlTestUtils {
    fun GraphQlTester.Response.satisfyNoErrors() = errors().satisfy { assertThat(it).isEmpty() }

    fun GraphQlTester.Errors.expectSingleError(
        errorType: ErrorType,
        code: String? = null,
        extensions: Map<String, Any?> = emptyMap(),
        path: String? = null
    ) = satisfy {
        assertThat(it).hasSize(1)

        val expectedExtensions = extensions
            .let { extensions -> if (code != null) extensions.plus("code" to code) else extensions }
            .plus("classification" to errorType.name)
        assertThat(it[0].extensions).isEqualTo(expectedExtensions)
        assertThat(it[0].path).isEqualTo(path)
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
}