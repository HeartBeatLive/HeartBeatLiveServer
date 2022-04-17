package com.munoon.heartbeatlive.server.auth

import com.munoon.heartbeatlive.server.subscription.JwtUserSubscription
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.time.Instant
import java.util.*

class CustomJwtAuthenticationToken(jwt: Jwt) : JwtAuthenticationToken(jwt, jwt.getAuthorities()) {
    val userId: String = name

    private val userSubscription = jwt.safeGetClaim<Map<String, Any>>(SUBSCRIPTION_PLAN_CLAIM)
        ?.let { JwtUserSubscription.createFromJwtClaimMap(it) }
        ?: JwtUserSubscription.DEFAULT

    val actualUserSubscriptionPlan: UserSubscriptionPlan
        get() {
            return when {
                userSubscription.expirationTime == null -> UserSubscriptionPlan.FREE
                Instant.now().isAfter(userSubscription.expirationTime) -> UserSubscriptionPlan.FREE
                else -> userSubscription.plan
            }
        }

    companion object {
        private val logger = LoggerFactory.getLogger(CustomJwtAuthenticationToken::class.java)

        const val SUBSCRIPTION_PLAN_CLAIM = "subscription_plan"
        const val ROLES_CLAIM = "roles"

        private fun Jwt.getAuthorities(): Set<UserRole> = (safeGetClaim<Set<String>>(ROLES_CLAIM) ?: emptySet())
            .mapTo(EnumSet.noneOf(UserRole::class.java)) { UserRole.valueOf(it) }

        private inline fun <reified T> Jwt.safeGetClaim(claimName: String): T? =
            try {
                val result = getClaim<T>(claimName)

                if (T::class.isInstance(result)) result
                else null
            } catch (e: Exception) {
                logger.error("Error getting JWT claim with name '$claimName'")
                null
            }
    }
}