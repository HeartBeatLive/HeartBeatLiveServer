package com.munoon.heartbeatlive.server.user.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserRecord
import com.munoon.heartbeatlive.server.auth.jwt.CustomJwtAuthenticationToken
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.UserEvents
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class FirebaseUserManager(private val firebaseAuth: FirebaseAuth) {
    @EventListener
    fun initializeFirebaseUser(event: UserEvents.UserCreatedEvent) {
        val user = event.user
        if (user.roles.isEmpty() && user.displayName == null) return

        firebaseAuth.updateUser(user.generateUpdateRequest())
    }

    @EventListener(condition = "#event.updateFirebaseState")
    fun updateFirebaseUser(event: UserEvents.UserUpdatedEvent) {
        val (newUser, oldUser) = event
        if (newUser.roles == oldUser.roles && newUser.displayName == oldUser.displayName) return

        val updateRequest = newUser.generateUpdateRequest()
        firebaseAuth.updateUser(updateRequest)
    }

    @EventListener(condition = "#event.updateFirebaseState")
    fun deleteFirebaseUser(event: UserEvents.UserDeletedEvent) {
        firebaseAuth.deleteUser(event.userId)
    }

    private companion object {
        fun User.generateClaims() = mapOf(
            CustomJwtAuthenticationToken.ROLES_CLAIM to roles.map { it.name }
        )

        fun User.generateUpdateRequest(): UserRecord.UpdateRequest = UserRecord.UpdateRequest(id)
            .setCustomClaims(generateClaims())
            .setDisplayName(displayName)
            .setPhotoUrl(null)
    }
}