package com.munoon.heartbeatlive.server.user.model

data class FirebaseCreateUserRequest(
    val id: String,
    val email: String?,
    val emailVerified: Boolean
)
