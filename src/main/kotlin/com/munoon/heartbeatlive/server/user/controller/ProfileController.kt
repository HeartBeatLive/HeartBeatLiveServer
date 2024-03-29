package com.munoon.heartbeatlive.server.user.controller

import com.munoon.heartbeatlive.server.auth.utils.AuthUtils.authUser
import com.munoon.heartbeatlive.server.auth.utils.AuthUtils.authUserId
import com.munoon.heartbeatlive.server.auth.utils.AuthUtils.authUserIdOrAnonymous
import com.munoon.heartbeatlive.server.user.UserMapper.asGraphqlProfile
import com.munoon.heartbeatlive.server.user.model.GraphqlProfileTo
import com.munoon.heartbeatlive.server.user.model.UpdateUserInfoFromJwtTo
import com.munoon.heartbeatlive.server.user.service.UserService
import org.hibernate.validator.constraints.Length
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import javax.validation.constraints.Email

@Controller
class ProfileController(private val userService: UserService) {
    private val logger = LoggerFactory.getLogger(ProfileController::class.java)

    @QueryMapping
    @PreAuthorize("permitAll()")
    suspend fun checkEmailReserved(@Argument @Email @Length(min = 1, max = 200) email: String): Boolean {
        logger.info("User '${authUserIdOrAnonymous()}' check if email '$email' is reserved")
        return userService.checkEmailReserved(email)
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    suspend fun updateProfileDisplayName(@Argument @Length(min = 2, max = 60) displayName: String): GraphqlProfileTo {
        logger.info("Updating user '${authUserId()}' display name to '$displayName'")
        return userService.updateUserDisplayName(authUserId(), displayName)
            .asGraphqlProfile()
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    suspend fun updateProfileInfo(): GraphqlProfileTo {
        logger.info("Updating user '${authUserId()}' info, provided by JWT token")
        val updateUserInfo = UpdateUserInfoFromJwtTo(emailVerified = authUser()!!.emailVerified)
        return userService.updateUserInfoFromJwt(authUserId(), updateUserInfo)
            .asGraphqlProfile()
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    suspend fun getProfile(): GraphqlProfileTo {
        logger.info("User '${authUserId()}' requested his profile")
        return userService.getUserById(authUserId()).asGraphqlProfile()
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    suspend fun deleteProfile(): Boolean {
        logger.info("User '${authUserId()}' delete his user")
        userService.deleteUserById(authUserId(), updateFirebaseState = true)
        return true
    }
}