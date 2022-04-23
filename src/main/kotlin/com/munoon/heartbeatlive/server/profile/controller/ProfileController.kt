package com.munoon.heartbeatlive.server.profile.controller

import com.munoon.heartbeatlive.server.auth.AuthUtils.authUserId
import com.munoon.heartbeatlive.server.auth.function.FirebaseFunctionAuthentication
import com.munoon.heartbeatlive.server.profile.UserService
import com.munoon.heartbeatlive.server.profile.model.FirebaseCreateUserRequest
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
class ProfileController(
    private val userService: UserService,
    private val firebaseFunctionAuthentication: FirebaseFunctionAuthentication
) {
    private val logger = LoggerFactory.getLogger(ProfileController::class.java)

    @QueryMapping
    @PreAuthorize("isAnonymous()")
    suspend fun checkEmailReserved(@Argument email: String): Boolean {
        logger.info("Checking if email '$email' is reserved")
        return userService.checkEmailReserved(email)
    }

    @MutationMapping
    @PreAuthorize("permitAll()")
    suspend fun firebaseCreateUser(@Argument request: FirebaseCreateUserRequest): Boolean {
        firebaseFunctionAuthentication.checkIsFirebaseFunction()
        logger.info("Received new user from firebase: $request")
        return userService.createUser(request)
    }

    @MutationMapping
    @PreAuthorize("permitAll()")
    suspend fun firebaseDeleteUser(@Argument userId: String): Boolean {
        firebaseFunctionAuthentication.checkIsFirebaseFunction()
        logger.info("Deleting user with id '$userId'")
        return userService.deleteUserById(userId)
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    suspend fun updateDisplayName(@Argument displayName: String): Boolean {
        logger.info("Updating user '${authUserId()}' display name to '$displayName'")
        return userService.updateUserDisplayName(authUserId()!!, displayName)
    }
}