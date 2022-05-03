package com.munoon.heartbeatlive.server.user.service

import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.UserNotFoundByIdException
import com.munoon.heartbeatlive.server.user.asNewUser
import com.munoon.heartbeatlive.server.user.firebase.FirebaseAuthService
import com.munoon.heartbeatlive.server.user.model.GraphqlFirebaseCreateUserInput
import com.munoon.heartbeatlive.server.user.model.UpdateUserInfoFromJwtTo
import com.munoon.heartbeatlive.server.user.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val firebaseAuthService: FirebaseAuthService
) {
    suspend fun checkEmailReserved(email: String): Boolean {
        return userRepository.existsByEmail(email.lowercase())
    }

    suspend fun createUser(request: GraphqlFirebaseCreateUserInput): User {
        val user = userRepository.save(request.asNewUser())

        firebaseAuthService.initializeFirebaseUser(user)
        return user
    }

    suspend fun deleteUserById(userId: String, updateFirebaseState: Boolean) {
        val deletedCount = userRepository.deleteUserById(userId)
        if (deletedCount <= 0) throw UserNotFoundByIdException(userId)
        if (updateFirebaseState) {
            firebaseAuthService.deleteFirebaseUser(userId)
        }
    }

    suspend fun updateUserDisplayName(userId: String, displayName: String): User {
        val user = getUserById(userId)
        val updatedUser = userRepository.save(user.copy(displayName = displayName))

        firebaseAuthService.updateFirebaseUser(updatedUser, user)
        return updatedUser
    }

    suspend fun updateUserInfoFromJwt(userId: String, updateUserInfo: UpdateUserInfoFromJwtTo): User {
        val user = getUserById(userId)
        return userRepository.save(user.copy(emailVerified = updateUserInfo.emailVerified))
    }

    suspend fun getUserById(userId: String): User = userRepository.findById(userId)
        ?: throw UserNotFoundByIdException(userId)
}