package com.munoon.heartbeatlive.server.config.error

import graphql.GraphQLError
import graphql.schema.DataFetchingEnvironment
import org.hibernate.validator.internal.engine.path.PathImpl
import org.slf4j.LoggerFactory
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.graphql.execution.ErrorType
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component
import java.util.*
import javax.validation.ConstraintViolationException
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

@Component
class CustomDataFetcherExceptionResolver : DataFetcherExceptionResolverAdapter() {
    private val logger = LoggerFactory.getLogger(CustomDataFetcherExceptionResolver::class.java)

    override fun resolveToSingleError(ex: Throwable, env: DataFetchingEnvironment): GraphQLError? = when (ex) {
        is ConstraintViolationException -> {
            val invalidProperties = ex.constraintViolations.mapNotNullTo(hashSetOf()) {
                (it.propertyPath as? PathImpl)?.iterator()?.let { nodesIterator ->
                    nodesIterator.next() // skipping first, as its method name
                    val stringJoiner = StringJoiner(".")
                    nodesIterator.forEachRemaining { node -> stringJoiner.add(node.name) }
                    stringJoiner.toString()
                }
            }

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
                message = settings.message.takeIf { it.isNotBlank() } ?: ex.message ?: "Exception happened!",
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