package com.munoon.heartbeatlive.server.utils

import org.assertj.core.api.Assertions.assertThat
import org.springframework.graphql.test.tester.GraphQlTester

object GraphqlTestUtils {
    fun GraphQlTester.Response.satisfyNoErrors() = errors().satisfy { assertThat(it).isEmpty() }

    inline fun <reified T : Any> GraphQlTester.Path.isEqualsTo(expected: T) = entity(T::class.java).isEqualTo(expected)
}