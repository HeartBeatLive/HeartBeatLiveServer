package com.munoon.heartbeatlive.server.user.controller

import com.munoon.heartbeatlive.server.auth.function.FirebaseFunctionAuthentication
import com.munoon.heartbeatlive.server.user.model.GraphqlFirebaseCreateUserInput
import com.munoon.heartbeatlive.server.user.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
class FirebaseUserController(
    private val userService: UserService,
    private val firebaseFunctionAuthentication: FirebaseFunctionAuthentication
) {
    private val logger = LoggerFactory.getLogger(FirebaseUserController::class.java)

    @MutationMapping
    @PreAuthorize("permitAll()")
    suspend fun firebaseCreateUser(@Argument request: GraphqlFirebaseCreateUserInput): Boolean {
        firebaseFunctionAuthentication.checkIsFirebaseFunction()
        logger.info("Received new user from firebase: $request")
        userService.createUser(request)
        return true
    }

    @MutationMapping
    @PreAuthorize("permitAll()")
    suspend fun firebaseDeleteUser(@Argument userId: String): Boolean {
        firebaseFunctionAuthentication.checkIsFirebaseFunction()
        logger.info("Deleting user with id '$userId'")
        userService.deleteUserByIdFirebaseTrigger(userId)
        return true
    }
}