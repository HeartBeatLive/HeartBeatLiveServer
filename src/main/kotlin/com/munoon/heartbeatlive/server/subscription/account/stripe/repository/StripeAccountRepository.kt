package com.munoon.heartbeatlive.server.subscription.account.stripe.repository

import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeAccount
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
interface StripeAccountRepository : CoroutineCrudRepository<StripeAccount, String> {
    suspend fun findByStripeAccountId(stripeAccountId: String): StripeAccount?
}