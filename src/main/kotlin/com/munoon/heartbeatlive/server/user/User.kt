package com.munoon.heartbeatlive.server.user

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("users")
data class User(
    @Id
    val id: String,

    val displayName: String?,

    @Indexed(unique = true)
    val email: String?,

    val emailVerified: Boolean,

    val created: Instant = Instant.now(), // TODO change?

    val roles: Set<UserRole> = emptySet()
) {
    companion object {
        const val UNIQUE_EMAIL_INDEX = "user_unique_email_index"
    }
}