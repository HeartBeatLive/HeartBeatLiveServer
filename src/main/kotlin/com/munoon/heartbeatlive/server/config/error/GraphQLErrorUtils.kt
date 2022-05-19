package com.munoon.heartbeatlive.server.config.error

import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.schema.DataFetchingEnvironment
import org.springframework.graphql.execution.ErrorType

object GraphQLErrorUtils {
    fun buildGraphQLError(
        env: DataFetchingEnvironment,
        errorType: ErrorType,
        code: String,
        message: String,
        extensions: Map<String, Any?> = emptyMap()
    ): GraphQLError {
        return GraphqlErrorBuilder.newError(env)
            .errorType(errorType)
            .message(message)
            .extensions(extensions.plus("code" to code))
            .build()
    }
}