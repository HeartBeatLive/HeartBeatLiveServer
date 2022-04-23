package com.munoon.heartbeatlive.server.profile.model

data class FirebaseCreateUserRequest(
    val id: String,
    val email: String?,
    val emailVerified: Boolean
)
