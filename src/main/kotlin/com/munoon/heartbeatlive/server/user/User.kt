package com.munoon.heartbeatlive.server.user

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
data class User(
    @Id
    val id: String,

    val displayName: String?,

    val email: String?,

    val emailVerified: Boolean,

    val created: Instant = Instant.now(), // TODO change?

    val roles: Set<UserRole> = emptySet()
)