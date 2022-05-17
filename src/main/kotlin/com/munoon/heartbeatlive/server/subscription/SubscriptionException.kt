package com.munoon.heartbeatlive.server.subscription

class UserSubscribersLimitExceededException : RuntimeException("User have too many subscribers.")

class UserSubscriptionsLimitExceededException : RuntimeException("User have too many subscriptions.")

class SelfSubscriptionAttemptException : RuntimeException("You cant subscribe to yourself.")

data class SubscriptionNotFoundByIdException(val id: String)
    : RuntimeException("Subscription with id '$id' is not found!")