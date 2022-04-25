package com.munoon.heartbeatlive.server.user.controller

import com.munoon.heartbeatlive.server.auth.AuthUtils.authUserId
import com.munoon.heartbeatlive.server.user.asPublicProfile
import com.munoon.heartbeatlive.server.user.model.PublicProfileTo
import com.munoon.heartbeatlive.server.user.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
class ProfileController(private val userService: UserService) {
    private val logger = LoggerFactory.getLogger(ProfileController::class.java)

    @QueryMapping
    @PreAuthorize("isAnonymous()")
    suspend fun checkEmailReserved(@Argument email: String): Boolean {
        logger.info("Checking if email '$email' is reserved")
        return userService.checkEmailReserved(email)
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    suspend fun updateDisplayName(@Argument displayName: String): PublicProfileTo {
        logger.info("Updating user '${authUserId()}' display name to '$displayName'")
        return userService.updateUserDisplayName(authUserId()!!, displayName)
            .asPublicProfile()
    }
}