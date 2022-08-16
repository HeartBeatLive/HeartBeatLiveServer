package com.munoon.heartbeatlive.server.subscription.account.stripe.service

import com.munoon.heartbeatlive.server.AbstractTest
import com.munoon.heartbeatlive.server.config.properties.SubscriptionProperties
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeAccount
import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeCustomerNotFoundByIdException
import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeMetadata
import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeRecurringChargeFailure
import com.munoon.heartbeatlive.server.subscription.account.stripe.client.StripeClient
import com.munoon.heartbeatlive.server.subscription.account.stripe.repository.StripeAccountRepository
import com.munoon.heartbeatlive.server.subscription.account.stripe.repository.StripeRecurringChargeFailureRepository
import com.munoon.heartbeatlive.server.user.User
import com.ninjasquad.springmockk.MockkBean
import com.stripe.model.Customer
import com.stripe.model.Event
import com.stripe.model.Invoice
import com.stripe.model.PaymentIntent
import com.stripe.model.Refund
import com.stripe.model.Subscription
import com.stripe.param.CustomerCreateParams
import com.stripe.param.RefundCreateParams
import com.stripe.param.SubscriptionCreateParams
import com.stripe.param.SubscriptionCreateParams.PaymentSettings.SaveDefaultPaymentMethod
import com.stripe.param.SubscriptionUpdateParams
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeUUID
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.MockKMatcherScope
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.event.ApplicationEvents
import org.springframework.test.context.event.RecordApplicationEvents
import java.time.Duration
import java.time.Instant

@SpringBootTest(properties = ["payment.stripe.payment-requires-action-window=PT23H"])
@RecordApplicationEvents
internal class StripeAccountSubscriptionServiceTest : AbstractTest() {
    @Autowired
    private lateinit var service: StripeAccountSubscriptionService

    @MockkBean
    private lateinit var client: StripeClient

    @Autowired
    private lateinit var accountRepository: StripeAccountRepository

    @Autowired
    private lateinit var recurringChargeFailureRepository: StripeRecurringChargeFailureRepository

    @Autowired
    private lateinit var applicationEvents: ApplicationEvents

    @Test
    fun createSubscription() {
        runBlocking { accountRepository.count() } shouldBe 0

        val stripeSubscription = Subscription().apply { id = "stripeSubscription1" }
        coEvery { client.createSubscription(any(), any()) } returns stripeSubscription
        coEvery { client.createCustomer(any(), any()) } returns Customer().apply { id = "stripeCustomer1" }

        val expectedCustomerParams = CustomerCreateParams.builder()
            .setEmail("email@example.com")
            .setName("User's Display Name")
            .setMetadata(mapOf("uid" to "user1"))
            .build()

        val expectedSubscriptionParams = SubscriptionCreateParams.builder()
            .setCustomer("stripeCustomer1")
            .addItem(SubscriptionCreateParams.Item.builder().setPrice("stripePrice1").build())
            .setPaymentSettings(
                SubscriptionCreateParams.PaymentSettings.builder()
                    .setSaveDefaultPaymentMethod(SaveDefaultPaymentMethod.ON_SUBSCRIPTION)
                    .build()
            )
            .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
            .setMetadata(mapOf(
                "refundSeconds" to "259200",
                "subscriptionPlan" to "PRO",
                "uid" to "user1"
            ))
            .addAllExpand(listOf("latest_invoice.payment_intent"))
            .build()

        val user = User(
            id = "user1",
            displayName = "User's Display Name",
            email = "email@example.com",
            emailVerified = false
        )

        val price = SubscriptionProperties.SubscriptionPrice().apply {
            stripePriceId = "stripePrice1"
            refundDuration = Duration.ofDays(3)
        }
        val subscription = runBlocking { service.createSubscription(UserSubscriptionPlan.PRO, price, user) }
        subscription shouldBe stripeSubscription

        coVerify(exactly = 1) { client.createSubscription(matchSubscriptionCreateParams(expectedSubscriptionParams), matchUUID()) }
        coVerify(exactly = 1) { client.createCustomer(matchCustomerCreateParams(expectedCustomerParams), matchUUID()) }
        runBlocking { accountRepository.findAll().toList(arrayListOf()) } shouldBe listOf(
            StripeAccount(id = "user1", stripeAccountId = "stripeCustomer1")
        )
    }

    @Test
    fun `createSubscription with existing stripe customer`() {
        runBlocking { accountRepository.save(StripeAccount(id = "user1", stripeAccountId = "stripeCustomer1")) }
        runBlocking { accountRepository.count() } shouldBe 1

        val stripeSubscription = Subscription().apply { id = "stripeSubscription1" }
        coEvery { client.createSubscription(any(), any()) } returns stripeSubscription

        val expectedSubscriptionParams = SubscriptionCreateParams.builder()
            .setCustomer("stripeCustomer1")
            .addItem(SubscriptionCreateParams.Item.builder().setPrice("stripePrice1").build())
            .setPaymentSettings(
                SubscriptionCreateParams.PaymentSettings.builder()
                    .setSaveDefaultPaymentMethod(SaveDefaultPaymentMethod.ON_SUBSCRIPTION)
                    .build()
            )
            .setMetadata(mapOf(
                "refundSeconds" to "259200",
                "subscriptionPlan" to "PRO",
                "uid" to "user1"
            ))
            .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
            .addAllExpand(listOf("latest_invoice.payment_intent"))
            .build()

        val user = User(
            id = "user1",
            displayName = "User's Display Name",
            email = "email@example.com",
            emailVerified = false
        )

        val price = SubscriptionProperties.SubscriptionPrice().apply {
            stripePriceId = "stripePrice1"
            refundDuration = Duration.ofDays(3)
        }
        val subscription = runBlocking { service.createSubscription(UserSubscriptionPlan.PRO, price, user) }
        subscription shouldBe stripeSubscription

        coVerify(exactly = 1) { client.createSubscription(matchSubscriptionCreateParams(expectedSubscriptionParams), matchUUID()) }
        coVerify(exactly = 0) { client.createCustomer(any(), any()) }
        runBlocking { accountRepository.count() } shouldBe 1
    }

    @Test
    fun handleEvent() {
        val event = Event().apply {
            id = "test_event_id"
            type = "test_event"
        }

        service.handleEvent(event)

        applicationEvents.stream(Event::class.java).toList() shouldBe listOf(event)
    }

    @Test
    fun getUserIdByStripeCustomerId(): Unit = runBlocking {
        checkAll(5, Arb.string(), Arb.string()) { userId, stripeCustomerId ->
            accountRepository.save(StripeAccount(id = userId, stripeAccountId = stripeCustomerId))
            service.getUserIdByStripeCustomerId(stripeCustomerId) shouldBe userId
        }
    }

    @Test
    fun `getUserIdByStripeCustomerId - stripe customer not found`() {
        runBlocking {
            shouldThrowExactly<StripeCustomerNotFoundByIdException> {
                service.getUserIdByStripeCustomerId("stripeCustomerId")
            } shouldBe StripeCustomerNotFoundByIdException("stripeCustomerId")
        }
    }

    @Test
    fun deleteCustomerByStripeId(): Unit = runBlocking {
        checkAll(5, Arb.string(), Arb.string()) { userId, stripeCustomerId ->
            accountRepository.save(StripeAccount(id = userId, stripeAccountId = stripeCustomerId))

            service.deleteCustomerByStripeId(stripeCustomerId)

            accountRepository.count() shouldBe 0
        }
    }

    @Test
    fun cancelUserSubscription() {
        coEvery { client.updateSubscription(any(), any(), any()) } returns Subscription()

        runBlocking { service.cancelUserSubscription("stripeSubscription1") }

        val updateParams = SubscriptionUpdateParams.builder().setCancelAtPeriodEnd(true).build()
        coVerify(exactly = 1) { client.updateSubscription(
            "stripeSubscription1", matchSubscriptionUpdateParams(updateParams), matchUUID()) }
    }

    @Test
    fun makeARefund() {
        val expectRefundParams = RefundCreateParams.builder()
            .setPaymentIntent("stripePaymentIntent1")
            .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
            .setMetadata(mapOf(StripeMetadata.Refund.USER_ID.addValue("user1")))
            .build()

        coEvery { client.createARefund(any(), any()) } returns Refund()
        coEvery { client.cancelSubscription(any(), any(), any()) } returns Subscription()

        runBlocking { service.makeARefund(
            userId = "user1",
            subscriptionId = "stripeSubscription1",
            paymentIntentId = "stripePaymentIntent1",
            reason = RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER
        ) }

        coVerify(exactly = 1) { client.createARefund(matchRefundCreateParams(expectRefundParams), matchUUID()) }
        coVerify(exactly = 1) { client.cancelSubscription("stripeSubscription1", null, matchUUID()) }
    }

    @Test
    fun saveFailedRecurringCharge() {
        val invoiceCreationTime = Instant.ofEpochSecond(Instant.now().epochSecond)

        val expected = StripeRecurringChargeFailure(
            userId = "user1",
            stripeInvoiceId = "stripeInvoice1",
            clientSecret = "stripeClientSecret",
            paymentIntentStatus = "requires_action",
            expiresAt = invoiceCreationTime + Duration.ofHours(23)
        )

        val invoice = Invoice().apply {
            paymentIntentObject = PaymentIntent().apply {
                clientSecret = "stripeClientSecret"
                status = "requires_action"
                created = invoiceCreationTime.epochSecond
            }
        }
        coEvery { client.getInvoice(any(), any(), any()) } returns invoice

        runBlocking { service.saveFailedRecurringCharge("user1", "stripeInvoice1") }

        val items = runBlocking { recurringChargeFailureRepository.findAll().toList(arrayListOf()) }
        items.shouldHaveSize(1)
        items.first().shouldBeEqualToIgnoringFields(expected, StripeRecurringChargeFailure::created)

        runBlocking { service.saveFailedRecurringCharge("user1", "stripeInvoice1") }

        items.shouldHaveSize(1)
        items.first().shouldBeEqualToIgnoringFields(expected, StripeRecurringChargeFailure::created)

        coVerify(exactly = 2) { client.getInvoice("stripeInvoice1", matchUUID(), "payment_intent") }
    }

    @Test
    fun getUserFailedRecurringCharge() {
        runBlocking {
            val expected = StripeRecurringChargeFailure(
                userId = "user1",
                stripeInvoiceId = "stripeInvoice1",
                clientSecret = "stripeClientSecret2",
                paymentIntentStatus = "requires_action",
                created = Instant.ofEpochSecond(Instant.now().epochSecond),
                expiresAt = Instant.ofEpochSecond(Instant.now().epochSecond) + Duration.ofHours(23)
            )

            recurringChargeFailureRepository.save(expected)
            recurringChargeFailureRepository.save(StripeRecurringChargeFailure(
                userId = "user2",
                stripeInvoiceId = "stripeInvoice2",
                clientSecret = "stripeClientSecret2",
                paymentIntentStatus = "requires_action",
                created = Instant.ofEpochSecond(Instant.now().epochSecond),
                expiresAt = Instant.ofEpochSecond(Instant.now().epochSecond) + Duration.ofHours(23)
            ))

            service.getUserFailedRecurringCharge("user1") shouldBe expected
            service.getUserFailedRecurringCharge("user3").shouldBeNull()
        }
    }

    @Test
    fun `getUserFailedRecurringCharge - expired`() {
        runBlocking {
            recurringChargeFailureRepository.save(StripeRecurringChargeFailure(
                userId = "user1",
                stripeInvoiceId = "stripeInvoice1",
                clientSecret = "stripeClientSecret1",
                paymentIntentStatus = "requires_action",
                created = Instant.ofEpochSecond(Instant.now().epochSecond),
                expiresAt = Instant.now().minusSeconds(10)
            ))

            service.getUserFailedRecurringCharge("user1").shouldBeNull()
        }
    }

    @Test
    fun cleanUserFailedRecurringCharge() {
        runBlocking {
            val expected = StripeRecurringChargeFailure(
                userId = "user1",
                stripeInvoiceId = "stripeInvoice1",
                clientSecret = "stripeClientSecret2",
                paymentIntentStatus = "requires_action",
                created = Instant.ofEpochSecond(Instant.now().epochSecond),
                expiresAt = Instant.ofEpochSecond(Instant.now().epochSecond) + Duration.ofHours(23)
            )

            recurringChargeFailureRepository.save(expected)
            recurringChargeFailureRepository.save(StripeRecurringChargeFailure(
                userId = "user2",
                stripeInvoiceId = "stripeInvoice2",
                clientSecret = "stripeClientSecret2",
                paymentIntentStatus = "requires_action",
                created = Instant.ofEpochSecond(Instant.now().epochSecond),
                expiresAt = Instant.ofEpochSecond(Instant.now().epochSecond) + Duration.ofHours(23)
            ))

            service.cleanUserFailedRecurringCharge("user2")

            val items = recurringChargeFailureRepository.findAll().toList(arrayListOf())
            items.shouldHaveSize(1)
            items.first() shouldBe expected

            service.cleanUserFailedRecurringCharge("user3")

            val items2 = recurringChargeFailureRepository.findAll().toList(arrayListOf())
            items2.shouldHaveSize(1)
            items2.first() shouldBe expected
        }
    }

    private companion object {
        fun MockKMatcherScope.matchSubscriptionCreateParams(expectedSubscription: SubscriptionCreateParams) = match<SubscriptionCreateParams> {
            assertThat(it).usingRecursiveComparison().isEqualTo(expectedSubscription)
            true
        }

        fun MockKMatcherScope.matchSubscriptionUpdateParams(expectedSubscription: SubscriptionUpdateParams) = match<SubscriptionUpdateParams> {
            assertThat(it).usingRecursiveComparison().isEqualTo(expectedSubscription)
            true
        }

        fun MockKMatcherScope.matchCustomerCreateParams(expectedCustomer: CustomerCreateParams) = match<CustomerCreateParams> {
            assertThat(it).usingRecursiveComparison().isEqualTo(expectedCustomer)
            true
        }

        fun MockKMatcherScope.matchRefundCreateParams(expectedRefund: RefundCreateParams) = match<RefundCreateParams> {
            assertThat(it).usingRecursiveComparison().isEqualTo(expectedRefund)
            true
        }

        fun MockKMatcherScope.matchUUID() = match<String> {
            it.shouldBeUUID()
            true
        }
    }
}