package com.munoon.heartbeatlive.server.subscription.account.stripe.controller

import com.munoon.heartbeatlive.server.AbstractGraphqlHttpTest
import com.munoon.heartbeatlive.server.config.properties.StripeConfigurationProperties
import com.munoon.heartbeatlive.server.config.properties.SubscriptionProperties
import com.munoon.heartbeatlive.server.subscription.account.JwtUserSubscription
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeRecurringChargeFailure
import com.munoon.heartbeatlive.server.subscription.account.stripe.model.GraphqlStripeRecurringChargeFailureInfo
import com.munoon.heartbeatlive.server.subscription.account.stripe.model.GraphqlStripeSubscription
import com.munoon.heartbeatlive.server.subscription.account.stripe.service.StripeAccountSubscriptionService
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.service.UserService
import com.munoon.heartbeatlive.server.utils.AuthTestUtils.withUser
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.expectSingleError
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.expectSingleUnauthenticatedError
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.isEqualsTo
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.satisfyNoErrors
import com.ninjasquad.springmockk.MockkBean
import com.stripe.model.Invoice
import com.stripe.model.PaymentIntent
import com.stripe.model.Subscription
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.execution.ErrorType
import java.time.Duration
import java.time.Instant

@SpringBootTest
internal class StripeAccountSubscriptionControllerTest : AbstractGraphqlHttpTest() {
    @MockkBean
    private lateinit var service: StripeAccountSubscriptionService

    @MockkBean
    private lateinit var properties: StripeConfigurationProperties

    @Autowired
    private lateinit var subscriptionProperties: SubscriptionProperties

    @MockkBean
    private lateinit var userService: UserService

    @BeforeEach
    fun setUpMocks() {
        every { properties.enabled } returns true
    }

    @Test
    fun createStripeSubscription() {
        val expectedStripeSubscription = GraphqlStripeSubscription(
            clientSecret = "stripeClientSecret"
        )

        val user = User(
            id = "user1",
            displayName = null,
            email = "email@example.com",
            emailVerified = true
        )
        coEvery { userService.getUserById(any()) } returns user

        coEvery { service.createSubscription(any(), any(), any()) } returns Subscription().apply {
            id = "stripeSubscriptionId"
            latestInvoiceObject = Invoice().apply {
                paymentIntentObject = PaymentIntent().apply {
                    clientSecret = "stripeClientSecret"
                }
            }
        }

        val price = subscriptionProperties[UserSubscriptionPlan.PRO].prices.first()
        val priceId = price.getId(UserSubscriptionPlan.PRO)

        graphqlTester.withUser(id = "user1")
            .document("""
                mutation {
                    createStripeSubscription(subscriptionPlanPriceId: "$priceId") {
                        clientSecret
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("createStripeSubscription").isEqualsTo(expectedStripeSubscription)

        coVerify(exactly = 1) { service.createSubscription(UserSubscriptionPlan.PRO, price, user) }
        coVerify(exactly = 1) { userService.getUserById("user1") }
    }

    @Test
    fun `createStripeSubscription - unsupported payment provider`() {
        every { properties.enabled } returns false

        graphqlTester.withUser()
            .document("""
                mutation {
                    createStripeSubscription(subscriptionPlanPriceId: "abc") {
                        clientSecret
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.FORBIDDEN,
                code = "payment.provider.unsupported",
                path = "createStripeSubscription",
                extensions = mapOf("providerName" to "STRIPE")
            )

        coVerify(exactly = 0) { service.createSubscription(any(), any(), any()) }
        coVerify(exactly = 0) { userService.getUserById(any()) }
    }

    @Test
    fun `createStripeSubscription - subscription price not found`() {
        graphqlTester.withUser(id = "user1")
            .document("""
                mutation {
                    createStripeSubscription(subscriptionPlanPriceId: "abc") {
                        clientSecret
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.NOT_FOUND,
                code = "account_subscription.subscription_plan.price.not_found.by_id",
                path = "createStripeSubscription",
                extensions = mapOf("id" to "abc")
            )

        coVerify(exactly = 0) { service.createSubscription(any(), any(), any()) }
        coVerify(exactly = 0) { userService.getUserById(any()) }
    }

    @Test
    fun `createStripeSubscription - user already subscribed (db request)`() {
        coEvery { userService.getUserById(any()) } returns User(
            id = "userId",
            displayName = null,
            email = "email@example.com",
            emailVerified = true,
            subscription = User.Subscription(
                plan = UserSubscriptionPlan.PRO,
                expiresAt = Instant.now().plusSeconds(60),
                startAt = Instant.now(),
                refundDuration = Duration.ofDays(3),
                details = User.Subscription.StripeSubscriptionDetails(
                    subscriptionId = "stripeSubscription1",
                    paymentIntentId = "stripePaymentIntent1"
                )
            )
        )

        val price = subscriptionProperties[UserSubscriptionPlan.PRO].prices.first()
        val priceId = price.getId(UserSubscriptionPlan.PRO)

        graphqlTester.withUser(id = "user1")
            .document("""
                mutation {
                    createStripeSubscription(subscriptionPlanPriceId: "$priceId") {
                        clientSecret
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.FORBIDDEN,
                code = "account_subscription.subscription_plan.user_already_subscribed",
                path = "createStripeSubscription"
            )

        coVerify(exactly = 0) { service.createSubscription(any(), any(), any()) }
        coVerify(exactly = 1) { userService.getUserById("user1") }
    }

    @Test
    fun `createStripeSubscription - user already subscribed (jwt token)`() {
        val price = subscriptionProperties[UserSubscriptionPlan.PRO].prices.first()
        val priceId = price.getId(UserSubscriptionPlan.PRO)

        val userSubscription = JwtUserSubscription(UserSubscriptionPlan.PRO, Instant.now().plusSeconds(60))
        graphqlTester.withUser(id = "user1", subscription = userSubscription)
            .document("""
                mutation {
                    createStripeSubscription(subscriptionPlanPriceId: "$priceId") {
                        clientSecret
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.FORBIDDEN,
                code = "account_subscription.subscription_plan.user_already_subscribed",
                path = "createStripeSubscription"
            )

        coVerify(exactly = 0) { service.createSubscription(any(), any(), any()) }
        coVerify(exactly = 0) { userService.getUserById(any()) }
    }

    @Test
    fun `createStripeSubscription - user doesnt have verified email address`() {
        coEvery { userService.getUserById(any()) } returns User(
            id = "user1",
            displayName = null,
            email = "email@example.com",
            emailVerified = false
        )

        val price = subscriptionProperties[UserSubscriptionPlan.PRO].prices.first()
        val priceId = price.getId(UserSubscriptionPlan.PRO)

        graphqlTester.withUser(id = "user1")
            .document("""
                mutation {
                    createStripeSubscription(subscriptionPlanPriceId: "$priceId") {
                        clientSecret
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.FORBIDDEN,
                code = "user.no_verified_email_address",
                path = "createStripeSubscription",
                extensions = mapOf("id" to "user1")
            )

        coVerify(exactly = 0) { service.createSubscription(any(), any(), any()) }
        coVerify(exactly = 1) { userService.getUserById("user1") }
    }

    @Test
    fun `createStripeSubscription - not authenticated`() {
        graphqlTester
            .document("""
                mutation {
                    createStripeSubscription(subscriptionPlanPriceId: "abc") {
                        clientSecret
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleUnauthenticatedError(path = "createStripeSubscription")

        coVerify(exactly = 0) { service.createSubscription(any(), any(), any()) }
        coVerify(exactly = 0) { userService.getUserById(any()) }
    }

    @Test
    fun getStripeRecurringChargeFailureInfo() {
        val recurringChargeFailureCreateTime = Instant.ofEpochSecond(Instant.now().epochSecond)
        val recurringChargeFailureExpiresTime = Instant.ofEpochSecond(Instant.now().plusSeconds(60).epochSecond)
        val expected = GraphqlStripeRecurringChargeFailureInfo(
            recurringChargeFailureCreateTime,
            recurringChargeFailureExpiresTime,
            "stripeClientSecret",
            GraphqlStripeRecurringChargeFailureInfo.FailureType.REQUIRES_ACTION
        )

        coEvery { service.getUserFailedRecurringCharge("user1") } returns StripeRecurringChargeFailure(
            userId = "user1",
            stripeInvoiceId = "stripeInvoiceId",
            clientSecret = "stripeClientSecret",
            paymentIntentStatus = "requires_action",
            created = recurringChargeFailureCreateTime,
            expiresAt = recurringChargeFailureExpiresTime
        )

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getStripeRecurringChargeFailureInfo {
                        createTime,
                        expireTime,
                        clientSecret,
                        failureType
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getStripeRecurringChargeFailureInfo").isEqualsTo(expected)

        coVerify(exactly = 1) { service.getUserFailedRecurringCharge("user1") }
    }

    @Test
    fun `getStripeRecurringChargeFailureInfo - null`() {
        coEvery { service.getUserFailedRecurringCharge("user1") } returns null

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getStripeRecurringChargeFailureInfo {
                        createTime,
                        expireTime,
                        clientSecret,
                        failureType
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getStripeRecurringChargeFailureInfo").valueIsNull()

        coVerify(exactly = 1) { service.getUserFailedRecurringCharge("user1") }
    }

    @Test
    fun `getStripeRecurringChargeFailureInfo - not authenticated`() {
        graphqlTester
            .document("""
                query {
                    getStripeRecurringChargeFailureInfo {
                        createTime,
                        expireTime,
                        clientSecret,
                        failureType
                    }
                }
            """.trimIndent())
            .execute()
            .errors().expectSingleUnauthenticatedError(path = "getStripeRecurringChargeFailureInfo")

        coVerify(exactly = 0) { service.getUserFailedRecurringCharge(any()) }
    }
}