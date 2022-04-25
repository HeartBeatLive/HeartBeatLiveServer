package com.munoon.heartbeatlive.server.subscription

import java.time.Instant

data class JwtUserSubscription(
    val plan: UserSubscriptionPlan,
    val expirationTime: Instant?
) {
    companion object {
        val DEFAULT = JwtUserSubscription(UserSubscriptionPlan.FREE, null)

        fun createFromJwtClaimMap(map: Map<String, Any>): JwtUserSubscription {
            val plan = map["plan"]
                ?.takeIf { it is String }
                ?.let { UserSubscriptionPlan.valueOf(it as String) }
                ?: return DEFAULT

            val expirationTime = map["exp"]
                ?.takeIf { it is Number }
                ?.let { Instant.ofEpochSecond((it as Number).toLong()) }
                ?: return DEFAULT

            return JwtUserSubscription(plan, expirationTime)
        }
    }

    fun asClaimsMap() = mapOf(
        "plan" to plan.name,
        "exp" to expirationTime?.epochSecond
    )
}