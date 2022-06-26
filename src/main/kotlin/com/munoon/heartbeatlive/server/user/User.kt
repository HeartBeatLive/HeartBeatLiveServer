package com.munoon.heartbeatlive.server.user

import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.security.core.GrantedAuthority
import java.time.Instant

@Document("users")
data class User(
    @Id
    val id: String,

    val displayName: String?,

    @Indexed(unique = true)
    val email: String?,

    val emailVerified: Boolean,

    val created: Instant = Instant.now(),

    val roles: Set<Role> = emptySet(),

    val subscription: Subscription? = null,

    val heartRates: List<HeartRate> = emptyList()
) {
    companion object {
        const val UNIQUE_EMAIL_INDEX = "user_unique_email_index"
    }

    enum class Role : GrantedAuthority {
        ADMIN;

        override fun getAuthority() = name
    }

    data class Subscription(
        val plan: UserSubscriptionPlan,
        val expiresAt: Instant
    )

    data class HeartRate(
        val heartRate: Int?, // null means user stopped sending heart rate
        val time: Instant
    )
}