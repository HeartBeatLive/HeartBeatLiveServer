package com.munoon.heartbeatlive.server.auth

import com.munoon.heartbeatlive.server.auth.AuthUtils.authUserId
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
class LogoutController {
    private val logger = LoggerFactory.getLogger(LogoutController::class.java)

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    suspend fun logout(): Boolean {
        // we can add some logic in future
        logger.info("User '${authUserId()}' logged out")
        return true
    }
}