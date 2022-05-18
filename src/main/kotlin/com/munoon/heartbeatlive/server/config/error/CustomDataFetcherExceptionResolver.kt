package com.munoon.heartbeatlive.server.config.error

import graphql.GraphQLError
import graphql.schema.DataFetchingEnvironment
import org.slf4j.LoggerFactory
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.graphql.execution.ErrorType
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component
import javax.validation.ConstraintViolationException
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

@Component
class CustomDataFetcherExceptionResolver : DataFetcherExceptionResolverAdapter() {
    private val logger = LoggerFactory.getLogger(CustomDataFetcherExceptionResolver::class.java)

    override fun resolveToSingleError(ex: Throwable, env: DataFetchingEnvironment): GraphQLError? = when (ex) {
        is ConstraintViolationException -> {
            val invalidProperties = ex.constraintViolations.mapTo(hashSetOf()) { it.propertyPath.toString() }

            GraphQLErrorUtils.buildGraphQLError(
                env = env,
                message = "Some validation rules are not passed.",
                errorType = ErrorType.BAD_REQUEST,
                code = "common.validation",
                extensions = mapOf("invalidProperties" to invalidProperties)
            )
        }

        is AccessDeniedException -> { GraphQLErrorUtils.buildGraphQLError(
            env = env,
            message = "Access denied.",
            errorType = ErrorType.FORBIDDEN,
            code = "common.access_denied"
        ) }

        else -> mapExceptionToError(ex, env) ?: createUnknownError(ex, env)
    }

    private fun mapExceptionToError(ex: Throwable, env: DataFetchingEnvironment): GraphQLError? {
        if (ex is GraphqlErrorBuildingException) {
            return ex.build(env)
        }

        ex::class.findAnnotation<ConvertExceptionToError>()?.let { settings ->
            val extensions = ex::class.declaredMemberProperties.associate { it.name to it.call(ex) }

            return GraphQLErrorUtils.buildGraphQLError(
                env = env,
                errorType = settings.type,
                code = settings.code,
                message = ex.message ?: "Exception happened!",
                extensions = extensions
            )
        }

        return null
    }

    private fun createUnknownError(ex: Throwable, env: DataFetchingEnvironment): GraphQLError {
        logger.error("Unknown exception in execution '${env.executionId}'", ex)
        return GraphQLErrorUtils.buildGraphQLError(
            env = env,
            errorType = ErrorType.INTERNAL_ERROR,
            code = "common.unknown",
            message = "Unknown exception"
        )
    }
}