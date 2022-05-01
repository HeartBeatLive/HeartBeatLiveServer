package com.munoon.heartbeatlive.server.user.repository

import com.munoon.heartbeatlive.server.user.User
import org.springframework.data.repository.kotlin.CoroutineSortingRepository

interface UserRepository : CoroutineSortingRepository<User, String> {
    suspend fun existsByEmail(email: String): Boolean

    suspend fun deleteUserById(id: String): Int
}