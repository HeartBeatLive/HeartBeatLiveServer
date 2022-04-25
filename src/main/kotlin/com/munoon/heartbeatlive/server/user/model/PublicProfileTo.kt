package com.munoon.heartbeatlive.server.user.model

data class PublicProfileTo(
    val id: String,
    val displayName: String?,
    val email: String?,
    val emailVerified: Boolean
)
