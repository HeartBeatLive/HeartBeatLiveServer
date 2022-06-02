@file:Suppress("MatchingDeclarationName")
package com.munoon.heartbeatlive.server.heartrate

data class TooManyHeartRateSubscriptionsExceptions(val userId: String, val limit: Int)
    : RuntimeException("User '$userId' have too many heart rate subscriptions (limit is $limit).")