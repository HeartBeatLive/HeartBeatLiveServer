package com.munoon.heartbeatlive.server.user.service

import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.UserEvents
import com.munoon.heartbeatlive.server.user.UserMapper.asNewUser
import com.munoon.heartbeatlive.server.user.UserNotFoundByIdException
import com.munoon.heartbeatlive.server.user.model.GraphqlFirebaseCreateUserInput
import com.munoon.heartbeatlive.server.user.model.UpdateUserInfoFromJwtTo
import com.munoon.heartbeatlive.server.user.repository.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class UserService(
    private val userRepository: UserRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    suspend fun checkEmailReserved(email: String): Boolean {
        return userRepository.existsByEmail(email.lowercase())
    }

    suspend fun createUser(request: GraphqlFirebaseCreateUserInput): User {
        val user = userRepository.save(request.asNewUser())
        eventPublisher.publishEvent(UserEvents.UserCreatedEvent(user))
        return user
    }

    suspend fun deleteUserById(userId: String, updateFirebaseState: Boolean) {
        val deletedCount = userRepository.deleteUserById(userId)
        if (deletedCount <= 0) throw UserNotFoundByIdException(userId)
        eventPublisher.publishEvent(UserEvents.UserDeletedEvent(userId, updateFirebaseState))
    }

    suspend fun updateUserDisplayName(userId: String, displayName: String): User {
        val user = getUserById(userId)
        val updatedUser = userRepository.save(user.copy(displayName = displayName))

        eventPublisher.publishEvent(UserEvents.UserUpdatedEvent(
            newUser = updatedUser,
            oldUser = user,
            updateFirebaseState = true
        ))

        return updatedUser
    }

    suspend fun updateUserInfoFromJwt(userId: String, updateUserInfo: UpdateUserInfoFromJwtTo): User {
        val user = getUserById(userId)
        val updatedUser = userRepository.save(user.copy(emailVerified = updateUserInfo.emailVerified))

        eventPublisher.publishEvent(UserEvents.UserUpdatedEvent(
            newUser = updatedUser,
            oldUser = user,
            updateFirebaseState = false
        ))

        return updatedUser
    }

    suspend fun getUserById(userId: String): User = userRepository.findById(userId)
        ?: throw UserNotFoundByIdException(userId)

    fun getUsersByIds(userIds: Set<String>) = userRepository.findAllById(userIds)

    suspend fun updateUserLastHeartRateReceiveTime(userId: String, receiveTime: Instant?): User {
        val user = getUserById(userId)
        val updatedUser = userRepository.save(user.copy(lastHeartRateInfoReceiveTime = receiveTime))

        eventPublisher.publishEvent(UserEvents.UserUpdatedEvent(
            newUser = updatedUser,
            oldUser = user,
            updateFirebaseState = false
        ))

        return updatedUser
    }
}