package com.munoon.heartbeatlive.server.user

import com.munoon.heartbeatlive.server.config.error.ConvertExceptionToError
import org.springframework.graphql.execution.ErrorType

data class UserNotFoundByIdException(val id: String)
    : RuntimeException("User with id '$id' is not found!")

@ConvertExceptionToError(
    type = ErrorType.FORBIDDEN,
    code = "user.no_verified_email_address",
    message = "You don't have verified email address."
)
data class NoUserVerifiedEmailAddressException(val id: String)
    : RuntimeException("User with id '$id' do not have verified email address!")