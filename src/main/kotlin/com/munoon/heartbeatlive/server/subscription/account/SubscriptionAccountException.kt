package com.munoon.heartbeatlive.server.subscription.account

import com.munoon.heartbeatlive.server.config.error.ConvertExceptionToError
import com.munoon.heartbeatlive.server.subscription.account.model.GraphqlPaymentProviderName
import org.springframework.graphql.execution.ErrorType

@ConvertExceptionToError(type = ErrorType.NOT_FOUND, code = "payment.provider.not_found")
class PaymentProviderNotFoundException : RuntimeException("No supported payment provider was found.")

@ConvertExceptionToError(type = ErrorType.FORBIDDEN, code = "payment.provider.unsupported")
data class PaymentProviderIsNotSupportedException(val providerName: GraphqlPaymentProviderName)
    : RuntimeException("Payment provider '$providerName' isn't currently supported!")

@ConvertExceptionToError(type = ErrorType.NOT_FOUND, code = "account_subscription.subscription_plan.not_found.by_code_name")
data class SubscriptionPlanNotFoundException(val name: String)
    : RuntimeException("Subscription plan '$name' is not found!")

@ConvertExceptionToError(type = ErrorType.NOT_FOUND, code = "account_subscription.subscription_plan.price.not_found.by_id")
data class SubscriptionPlanPriceIsNotFoundByIdException(val id: String)
    : RuntimeException("Subscription plan price with id '$id' is not found!")

@ConvertExceptionToError(type = ErrorType.FORBIDDEN, code = "account_subscription.subscription_plan.user_already_subscribed")
class UserAlreadyHaveActiveSubscriptionException : RuntimeException("User already have active subscription.")

@ConvertExceptionToError(type = ErrorType.FORBIDDEN, code = "account_subscription.subscription_plan.user_have_no_active_subscription")
class UserHaveNotActiveSubscriptionException : RuntimeException("User haven't any active subscription.")