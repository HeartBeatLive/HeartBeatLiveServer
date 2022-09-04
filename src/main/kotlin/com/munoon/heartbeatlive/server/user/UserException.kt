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

@ConvertExceptionToError(type = ErrorType.NOT_FOUND, code = "user.not_found.by_email")
data class UserNotFoundByEmailException(val email: String) : RuntimeException("User with email '$email' is not found!")

@ConvertExceptionToError(type = ErrorType.FORBIDDEN, code = "user.reset_password_request.already_made")
class ResetPasswordRequestAlreadyMadeException : RuntimeException("You already create a request to reset a password. " +
        "Please, wait a bit before making new request!")