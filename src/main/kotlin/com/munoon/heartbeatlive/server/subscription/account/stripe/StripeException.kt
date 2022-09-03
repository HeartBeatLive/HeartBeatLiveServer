@file:Suppress("MatchingDeclarationName")
package com.munoon.heartbeatlive.server.subscription.account.stripe

data class StripeCustomerNotFoundByIdException(val customerId: String)
    : RuntimeException("Stripe customer with id '$customerId' is not found!")