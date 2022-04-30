package com.munoon.heartbeatlive.server.user.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserRecord
import com.munoon.heartbeatlive.server.auth.jwt.CustomJwtAuthenticationToken
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.firebase.FirebaseUtils.await
import org.springframework.stereotype.Service

@Service
class FirebaseAuthService(private val firebaseAuth: FirebaseAuth) {
    suspend fun updateFirebaseUser(newUser: User, oldUser: User) {
        if (newUser.roles == oldUser.roles && newUser.displayName == oldUser.displayName) return

        val updateRequest = newUser.generateUpdateRequest()
        firebaseAuth.updateUserAsync(updateRequest).await()
    }

    suspend fun initializeFirebaseUser(user: User) {
        if (user.roles.isEmpty() && user.displayName == null) return

        firebaseAuth.updateUserAsync(user.generateUpdateRequest()).await()
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