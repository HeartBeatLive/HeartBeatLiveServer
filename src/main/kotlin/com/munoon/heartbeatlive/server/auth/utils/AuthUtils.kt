package com.munoon.heartbeatlive.server.auth.utils

import com.munoon.heartbeatlive.server.auth.jwt.CustomJwtAuthenticationToken
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.security.core.context.ReactiveSecurityContextHolder

object AuthUtils {
    suspend fun authUser() = ReactiveSecurityContextHolder.getContext()
        .map { it.authentication as CustomJwtAuthenticationToken }
        .awaitSingleOrNull()

    suspend fun authUserIdOrAnonymous(): String = authUser()?.userId ?: "<anonymous>"

    suspend fun authUserId(): String = authUser()!!.userId

    suspend fun authUserSubscription(): UserSubscriptionPlan = authUser()!!.actualUserSubscriptionPlan
}