package com.munoon.heartbeatlive.server.subscription

import com.munoon.heartbeatlive.server.config.error.ConvertExceptionToError
import org.springframework.graphql.execution.ErrorType

@ConvertExceptionToError(type = ErrorType.FORBIDDEN, code = "subscription.subscribers_limit_exceeded")
class UserSubscribersLimitExceededException : RuntimeException("User have too many subscribers.")

@ConvertExceptionToError(type = ErrorType.FORBIDDEN, code = "subscription.subscriptions_limit_exceeded")
class UserSubscriptionsLimitExceededException : RuntimeException("User have too many subscriptions.")

@ConvertExceptionToError(type = ErrorType.BAD_REQUEST, code = "subscription.self_subscribe")
class SelfSubscriptionAttemptException : RuntimeException("You cant subscribe to yourself.")

@ConvertExceptionToError(type = ErrorType.NOT_FOUND, code = "subscription.not_found.by_id")
data class SubscriptionNotFoundByIdException(val id: String)
    : RuntimeException("Subscription with id '$id' is not found!")