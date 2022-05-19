package com.munoon.heartbeatlive.server.ban

import com.munoon.heartbeatlive.server.config.error.ConvertExceptionToError
import com.munoon.heartbeatlive.server.config.error.GraphQLErrorUtils
import com.munoon.heartbeatlive.server.config.error.GraphqlErrorBuildingException
import graphql.GraphQLError
import graphql.schema.DataFetchingEnvironment
import org.springframework.graphql.execution.ErrorType

data class UserBanedByOtherUserException(val userId: String, val bannedByUserId: String)
    : RuntimeException("User '$userId' is banned by user '$bannedByUserId'"), GraphqlErrorBuildingException {
    override fun build(env: DataFetchingEnvironment): GraphQLError = GraphQLErrorUtils.buildGraphQLError(
        env = env,
        errorType = ErrorType.FORBIDDEN,
        message = "You are banned by this user.",
        code = "ban.banned"
    )
}

@ConvertExceptionToError(type = ErrorType.NOT_FOUND, code = "ban.not_found.by_id")
data class UserBanNotFoundByIdException(val id: String)
    : RuntimeException("User ban with id '$id' is not found!")