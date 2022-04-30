package com.munoon.heartbeatlive.server.user.model

data class GraphqlFirebaseCreateUserInput(
    val id: String,
    val email: String?,
    val emailVerified: Boolean
)
