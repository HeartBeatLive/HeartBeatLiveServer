package com.munoon.heartbeatlive.server.user.controller

import com.munoon.heartbeatlive.server.auth.utils.AuthUtils.authUserIdOrAnonymous
import com.munoon.heartbeatlive.server.email.ResetPasswordEmailMessage
import com.munoon.heartbeatlive.server.email.service.EmailService
import com.munoon.heartbeatlive.server.user.ResetPasswordRequestAlreadyMadeException
import com.munoon.heartbeatlive.server.user.UserNotFoundByEmailException
import com.munoon.heartbeatlive.server.user.firebase.FirebaseUserManager
import com.munoon.heartbeatlive.server.user.repository.UserResetEmailRequestRepository
import com.munoon.heartbeatlive.server.user.service.UserService
import graphql.GraphQLContext
import org.hibernate.validator.constraints.Length
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.execution.ReactorContextManager
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.web.server.ServerWebExchange
import reactor.util.context.ContextView
import java.time.Instant
import javax.validation.constraints.Email

@Controller
@PreAuthorize("permitAll()")
class ResetPasswordEmailController(
    private val userService: UserService,
    private val firebaseUserManager: FirebaseUserManager,
    private val emailService: EmailService,
    private val userResetEmailRequestRepository: UserResetEmailRequestRepository
) {
    private val logger = LoggerFactory.getLogger(ResetPasswordEmailController::class.java)

    @MutationMapping
    suspend fun sendResetPasswordEmail(
        @Argument @Email @Length(min = 1, max = 200) email: String,
        context: GraphQLContext
    ): Boolean {
        logger.info("User '${authUserIdOrAnonymous()}' request an email to '$email' to reset a password")

        val clientIpAddress = context.get<ContextView>(ReactorContextManager::class.java.name + ".CONTEXT_VIEW")
            .get(ServerWebExchange::class.java)
            .request.remoteAddress?.address?.hostAddress ?: return false

        if (!userService.checkEmailReserved(email)) {
            throw UserNotFoundByEmailException(email)
        }

        if (userResetEmailRequestRepository.checkIfIpAddressMadeARequest(clientIpAddress)) {
            throw ResetPasswordRequestAlreadyMadeException()
        }

        userResetEmailRequestRepository.saveANewRequestForIpAddress(clientIpAddress, Instant.now())

        val resetLink = firebaseUserManager.generatePasswordResetLink(email)
        emailService.send(ResetPasswordEmailMessage(email, resetLink))
        return true
    }
}