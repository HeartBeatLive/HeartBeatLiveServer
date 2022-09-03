package com.munoon.heartbeatlive.server.auth.jwt

import com.munoon.heartbeatlive.server.subscription.account.AccountSubscriptionUtils.getActiveSubscriptionPlan
import com.munoon.heartbeatlive.server.subscription.account.JwtUserSubscription
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import com.munoon.heartbeatlive.server.user.User.Role
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.util.*

class CustomJwtAuthenticationToken(jwt: Jwt) : JwtAuthenticationToken(jwt, jwt.getAuthorities()) {
    val userId: String = name

    private val userSubscription = jwt.safeGetClaim<Map<String, Any>>(SUBSCRIPTION_PLAN_CLAIM)
        ?.let { JwtUserSubscription.createFromJwtClaimMap(it) }
        ?: JwtUserSubscription.DEFAULT

    val actualUserSubscriptionPlan: UserSubscriptionPlan
        get() = userSubscription.getActiveSubscriptionPlan()

    val email: String = jwt.getClaimAsString(EMAIL_CLAIM)
    val emailVerified: Boolean = jwt.getClaimAsBoolean(EMAIL_VERIFIED_CLAIM) ?: false

    companion object {
        private val logger = LoggerFactory.getLogger(CustomJwtAuthenticationToken::class.java)

        const val SUBSCRIPTION_PLAN_CLAIM = "subscription_plan"
        const val ROLES_CLAIM = "roles"
        const val EMAIL_CLAIM = "email"
        const val EMAIL_VERIFIED_CLAIM = "email_verified"

        private fun Jwt.getAuthorities(): Set<Role> = (safeGetClaim<Set<String>>(ROLES_CLAIM) ?: emptySet())
            .mapTo(EnumSet.noneOf(Role::class.java)) { Role.valueOf(it) }

        private inline fun <reified T> Jwt.safeGetClaim(claimName: String): T? =
            try {
                val result = getClaim<T>(claimName)

                if (T::class.isInstance(result)) result
                else null
            } catch (e: Exception) {
                logger.error("Error getting JWT claim with name '$claimName'", e)
                null
            }
    }
}