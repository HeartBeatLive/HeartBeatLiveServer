package com.munoon.heartbeatlive.server.user.controller

import com.munoon.heartbeatlive.server.AbstractGraphqlHttpTest
import com.munoon.heartbeatlive.server.email.ResetPasswordEmailMessage
import com.munoon.heartbeatlive.server.email.service.EmailService
import com.munoon.heartbeatlive.server.user.firebase.FirebaseUserManager
import com.munoon.heartbeatlive.server.user.repository.UserResetEmailRequestRepository
import com.munoon.heartbeatlive.server.user.service.UserService
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.expectSingleError
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.expectSingleValidationError
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.isEqualsTo
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.satisfyNoErrors
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.withRemoteAddress
import com.ninjasquad.springmockk.MockkBean
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.execution.ErrorType
import java.net.InetSocketAddress
import java.time.Instant

@SpringBootTest
internal class ResetPasswordEmailControllerTest : AbstractGraphqlHttpTest() {
    @MockkBean
    private lateinit var userService: UserService

    @MockkBean
    private lateinit var firebaseUserManager: FirebaseUserManager

    @MockkBean
    private lateinit var emailService: EmailService

    @Autowired
    private lateinit var userResetEmailRequestRepository: UserResetEmailRequestRepository

    @Test
    fun sendResetPasswordEmail() {
        val resetPasswordLink = "https://example.com/resetPassword"
        val emailAddress = "email@example.com"
        val clientIpAddress = "127.0.0.1"

        coEvery { userService.checkEmailReserved(any()) } returns true
        coEvery { firebaseUserManager.generatePasswordResetLink(any()) } returns resetPasswordLink
        coEvery { emailService.send(any()) } returns Unit

        graphqlTester.withRemoteAddress(InetSocketAddress(clientIpAddress, 80))
            .document("""
                mutation {
                    sendResetPasswordEmail(email: "$emailAddress")
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("sendResetPasswordEmail").isEqualsTo(true)

        coVerify(exactly = 1) { userService.checkEmailReserved(emailAddress) }
        coVerify(exactly = 1) { firebaseUserManager.generatePasswordResetLink(emailAddress) }
        coVerify(exactly = 1) { emailService.send(ResetPasswordEmailMessage(emailAddress, resetPasswordLink)) }
        runBlocking { userResetEmailRequestRepository.checkIfIpAddressMadeARequest(clientIpAddress) } shouldBe true
    }

    @Test
    fun `sendResetPasswordEmail - user not found`() {
        val emailAddress = "email@example.com"
        val clientIpAddress = "127.0.0.1"

        coEvery { userService.checkEmailReserved(any()) } returns false

        graphqlTester.withRemoteAddress(InetSocketAddress(clientIpAddress, 80))
            .document("""
                mutation {
                    sendResetPasswordEmail(email: "$emailAddress")
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.NOT_FOUND,
                code = "user.not_found.by_email",
                extensions = mapOf("email" to "email@example.com"),
                path = "sendResetPasswordEmail"
            )

        coVerify(exactly = 1) { userService.checkEmailReserved(emailAddress) }
        coVerify(exactly = 0) { firebaseUserManager.generatePasswordResetLink(any()) }
        coVerify(exactly = 0) { emailService.send(any()) }
        runBlocking { userResetEmailRequestRepository.checkIfIpAddressMadeARequest(clientIpAddress) } shouldBe false
    }

    @Test
    fun `sendResetPasswordEmail - request already created`() {
        val emailAddress = "email@example.com"
        val clientIpAddress = "127.0.0.1"

        runBlocking { userResetEmailRequestRepository.saveANewRequestForIpAddress(clientIpAddress, Instant.now()) }

        coEvery { userService.checkEmailReserved(any()) } returns true

        graphqlTester.withRemoteAddress(InetSocketAddress(clientIpAddress, 80))
            .document("""
                mutation {
                    sendResetPasswordEmail(email: "$emailAddress")
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.FORBIDDEN,
                code = "user.reset_password_request.already_made",
                path = "sendResetPasswordEmail"
            )

        coVerify(exactly = 1) { userService.checkEmailReserved(emailAddress) }
        coVerify(exactly = 0) { firebaseUserManager.generatePasswordResetLink(any()) }
        coVerify(exactly = 0) { emailService.send(any()) }
        runBlocking { userResetEmailRequestRepository.checkIfIpAddressMadeARequest(clientIpAddress) } shouldBe true
    }

    @Test
    fun `sendResetPasswordEmail - ip address not found`() {
        graphqlTester
            .document("""
                mutation {
                    sendResetPasswordEmail(email: "email@example.com")
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("sendResetPasswordEmail").isEqualsTo(false)

        coVerify(exactly = 0) { userService.checkEmailReserved(any()) }
        coVerify(exactly = 0) { firebaseUserManager.generatePasswordResetLink(any()) }
        coVerify(exactly = 0) { emailService.send(any()) }
    }

    @Test
    fun `sendResetPasswordEmail - email address not valid`() {
        val clientIpAddress = "127.0.0.1"

        graphqlTester.withRemoteAddress(InetSocketAddress(clientIpAddress, 80))
            .document("""
                mutation {
                    sendResetPasswordEmail(email: "abc")
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleValidationError(path = "sendResetPasswordEmail", "email")

        coVerify(exactly = 0) { userService.checkEmailReserved(any()) }
        coVerify(exactly = 0) { firebaseUserManager.generatePasswordResetLink(any()) }
        coVerify(exactly = 0) { emailService.send(any()) }
        runBlocking { userResetEmailRequestRepository.checkIfIpAddressMadeARequest(clientIpAddress) } shouldBe false
    }
}