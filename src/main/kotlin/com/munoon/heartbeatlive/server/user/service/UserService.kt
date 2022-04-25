package com.munoon.heartbeatlive.server.user.service

import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.UserNotFoundByIdException
import com.munoon.heartbeatlive.server.user.firebase.FirebaseAuthService
import com.munoon.heartbeatlive.server.user.model.FirebaseCreateUserRequest
import com.munoon.heartbeatlive.server.user.repository.UserRepository
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val firebaseAuthService: FirebaseAuthService
) {
    suspend fun checkEmailReserved(email: String): Boolean {
        return userRepository.existsByEmail(email)
    }

    suspend fun createUser(request: FirebaseCreateUserRequest): User {
        val user = userRepository.save(User(
            id = request.id,
            displayName = null,
            email = request.email,
            emailVerified = request.emailVerified
        )).awaitSingle()

        firebaseAuthService.initializeFirebaseUser(user)
        return user
    }

    suspend fun deleteUserByIdFirebaseTrigger(userId: String) {
        val deletedCount = userRepository.deleteUserById(userId)
        if (deletedCount <= 0) throw UserNotFoundByIdException(userId)
    }

    suspend fun updateUserDisplayName(userId: String, displayName: String): User {
        val user = getUserById(userId)
        val updatedUser = userRepository.save(user.copy(displayName = displayName))
            .awaitSingle()

        firebaseAuthService.updateFirebaseUser(updatedUser, user)
        return updatedUser
    }

    suspend fun getUserById(userId: String): User = userRepository.findById(userId).awaitSingleOrNull()
        ?: throw UserNotFoundByIdException(userId)
}