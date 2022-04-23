package com.munoon.heartbeatlive.server.profile

import com.munoon.heartbeatlive.server.profile.model.FirebaseCreateUserRequest
import org.springframework.stereotype.Service

@Service
class UserService {
    suspend fun checkEmailReserved(email: String): Boolean {
        TODO("Not yet implemented")
    }

    suspend fun createUser(request: FirebaseCreateUserRequest): Boolean {
        TODO("Not yet implemented")
    }

    suspend fun deleteUserById(userId: String): Boolean {
        TODO("Not yet implemented")
    }

    suspend fun updateUserDisplayName(authUserId: String, displayName: String): Boolean {
        TODO("Not yet implemented")
    }
}