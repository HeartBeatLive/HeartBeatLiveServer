package com.munoon.heartbeatlive.server.config.error

import graphql.GraphqlErrorBuilder
import graphql.execution.ResultPath
import graphql.language.SourceLocation
import graphql.schema.DataFetchingEnvironment
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.graphql.execution.ErrorType

internal class GraphQLErrorUtilsTest {
    @Test
    fun buildGraphQLError() {
        val environment = mockk<DataFetchingEnvironment>() {
            every { field } returns mockk() {
                every { sourceLocation } returns SourceLocation(1, 2)
            }

            every { executionStepInfo } returns mockk() {
                every { path } returns ResultPath.rootPath()
            }
        }

        val expected = GraphqlErrorBuilder.newError()
            .location(SourceLocation(1, 2))
            .path(ResultPath.rootPath())
            .errorType(ErrorType.FORBIDDEN)
            .extensions(mapOf("code" to "test_code", "a" to 1, "b" to "abc"))
            .message("Test message")

        val actual = GraphQLErrorUtils.buildGraphQLError(
            env = environment,
            errorType = ErrorType.FORBIDDEN,
            code = "test_code",
            message = "Test message",
            extensions = mapOf("a" to 1, "b" to "abc")
        )

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
    }
}