package com.munoon.heartbeatlive.server.config.error

import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.execution.ExecutionId
import graphql.execution.ResultPath
import graphql.language.SourceLocation
import graphql.schema.DataFetchingEnvironment
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.validator.internal.engine.path.PathImpl
import org.junit.jupiter.api.Test
import org.springframework.graphql.execution.ErrorType
import org.springframework.security.access.AccessDeniedException
import javax.validation.ConstraintViolationException

internal class CustomDataFetcherExceptionResolverTest {
    private companion object {
        val environment = mockk<DataFetchingEnvironment>() {
            every { field } returns mockk() {
                every { sourceLocation } returns SourceLocation(1, 2)
            }

            every { executionStepInfo } returns mockk() {
                every { path } returns ResultPath.rootPath()
            }

            every { executionId } returns ExecutionId.generate()
        }

        fun assertMatch(actual: List<GraphQLError>?, vararg expected: GraphQLError) {
            assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("message")
                .isEqualTo(expected.toList())
        }
    }

    private val resolver = CustomDataFetcherExceptionResolver()

    @Test
    fun `resolve ConstrainViolationException`() {
        val expectedError = GraphqlErrorBuilder.newError()
            .location(SourceLocation(1, 2))
            .path(ResultPath.rootPath())
            .errorType(ErrorType.BAD_REQUEST)
            .extensions(mapOf("code" to "common.validation", "invalidProperties" to setOf("testPropertyPath.property")))
            .message("ignore")
            .build()

        val exception = ConstraintViolationException("Message", setOf(
            mockk() {
                every { propertyPath } returns PathImpl.createPathFromString("testPropertyPath.property")
            }
        ))

        val errors = resolver.resolveException(exception, environment).block()
        assertMatch(errors, expectedError)
    }

    @Test
    fun `resolve AccessDeniedException`() {
        val expectedError = GraphqlErrorBuilder.newError()
            .location(SourceLocation(1, 2))
            .path(ResultPath.rootPath())
            .errorType(ErrorType.FORBIDDEN)
            .extensions(mapOf("code" to "common.access_denied"))
            .message("ignore")
            .build()

        val exception = AccessDeniedException("Test message")

        val errors = resolver.resolveException(exception, environment).block()
        assertMatch(errors, expectedError)
    }

    @Test
    fun `resolve unknown exception`() {
        val expectedError = GraphqlErrorBuilder.newError()
            .location(SourceLocation(1, 2))
            .path(ResultPath.rootPath())
            .errorType(ErrorType.INTERNAL_ERROR)
            .extensions(mapOf("code" to "common.unknown"))
            .message("ignore")
            .build()

        val exception = UnknownException()

        val errors = resolver.resolveException(exception, environment).block()
        assertMatch(errors, expectedError)
    }

    @Test
    fun `resolve exception with @ConvertExceptionToError`() {
        val expectedError = GraphqlErrorBuilder.newError()
            .location(SourceLocation(1, 2))
            .path(ResultPath.rootPath())
            .errorType(ErrorType.BAD_REQUEST)
            .extensions(mapOf("code" to "test_code", "a" to "testA", "b" to 123))
            .message("ignore")
            .build()

        val exception = ConvertingToErrorUsingAnnotationException(a = "testA", b = 123)

        val errors = resolver.resolveException(exception, environment).block()
        assertMatch(errors, expectedError)
    }

    @Test
    fun `resolve GraphqlErrorBuildingException exception`() {
        val expectedError = GraphqlErrorBuilder.newError()
            .location(SourceLocation(1, 2))
            .path(ResultPath.rootPath())
            .errorType(ErrorType.NOT_FOUND)
            .message("ignore")
            .build()

        val exception = ConvertingToErrorUsingBuilderException()

        val errors = resolver.resolveException(exception, environment).block()
        assertMatch(errors, expectedError)
    }

    class UnknownException : Throwable()

    @ConvertExceptionToError(type = ErrorType.BAD_REQUEST, code = "test_code")
    data class ConvertingToErrorUsingAnnotationException(val a: String, val b: Int) : Throwable("Test message")

    class ConvertingToErrorUsingBuilderException : Throwable(), GraphqlErrorBuildingException {
        override fun build(env: DataFetchingEnvironment): GraphQLError {
            return GraphqlErrorBuilder.newError(env)
                .message("test error")
                .errorType(ErrorType.NOT_FOUND)
                .build()
        }
    }
}