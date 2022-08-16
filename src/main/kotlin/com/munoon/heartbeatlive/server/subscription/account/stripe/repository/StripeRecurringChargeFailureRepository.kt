package com.munoon.heartbeatlive.server.subscription.account.stripe.repository

import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeRecurringChargeFailure
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
interface StripeRecurringChargeFailureRepository : CoroutineCrudRepository<StripeRecurringChargeFailure, String>