package com.munoon.heartbeatlive.server.user

sealed interface UserEvents {
    val userId: String

    data class UserDeletedEvent(
        override val userId: String,
        val updateFirebaseState: Boolean
    ) : UserEvents

    data class UserCreatedEvent(
        val user: User
    ) : UserEvents {
        override val userId: String = user.id
    }

    data class UserUpdatedEvent(
        val newUser: User,
        val oldUser: User,
        val updateFirebaseState: Boolean
    ) : UserEvents {
        override val userId: String = newUser.id
    }
}