package com.munoon.heartbeatlive.server.subscription.account.stripe.service

import com.munoon.heartbeatlive.server.AbstractTest
import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeAccount
import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeCustomerNotFoundByIdException
import com.munoon.heartbeatlive.server.subscription.account.stripe.client.StripeClient
import com.munoon.heartbeatlive.server.subscription.account.stripe.repository.StripeAccountRepository
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.service.UserService
import com.ninjasquad.springmockk.MockkBean
import com.stripe.model.Customer
import com.stripe.model.Event
import com.stripe.model.Subscription
import com.stripe.param.CustomerCreateParams
import com.stripe.param.SubscriptionCreateParams
import com.stripe.param.SubscriptionCreateParams.PaymentSettings.SaveDefaultPaymentMethod
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
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

@SpringBootTest
@RecordApplicationEvents
internal class StripeAccountSubscriptionServiceTest : AbstractTest() {
    @Autowired
    private lateinit var service: StripeAccountSubscriptionService

    @MockkBean
    private lateinit var client: StripeClient

    @MockkBean
    private lateinit var userService: UserService

    @Autowired
    private lateinit var accountRepository: StripeAccountRepository

    @Autowired
    private lateinit var applicationEvents: ApplicationEvents

    @Test
    fun createSubscription() {
        runBlocking { accountRepository.count() } shouldBe 0

        val stripeSubscription = Subscription().apply { id = "stripeSubscription1" }
        coEvery { client.createSubscription(any(), any()) } returns stripeSubscription
        coEvery { client.createCustomer(any(), any()) } returns Customer().apply { id = "stripeCustomer1" }

        coEvery { userService.getUserById("user1") } returns User(
            id = "user1",
            displayName = "User's Display Name",
            email = "email@example.com",
            emailVerified = false
        )

        val expectedCustomerParams = CustomerCreateParams.builder()
            .setEmail("email@example.com")
            .setName("User's Display Name")
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
            .addAllExpand(listOf("latest_invoice.payment_intent"))
            .build()

        val subscription = runBlocking { service.createSubscription("stripePrice1", "user1") }
        subscription shouldBe stripeSubscription

        coVerify(exactly = 1) { client.createSubscription(matchSubscriptionCreateParams(expectedSubscriptionParams), any()) }
        coVerify(exactly = 1) { client.createCustomer(matchCustomerCreateParams(expectedCustomerParams), any()) }
        coVerify(exactly = 1) { userService.getUserById("user1") }
        runBlocking { accountRepository.findAll().toList(arrayListOf()) } shouldBe listOf(
            StripeAccount(id = "user1", stripeAccountId = "stripeCustomer1")
        )
    }

    @Test
    fun `createSubscription with exist stripe customer`() {
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
            .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
            .addAllExpand(listOf("latest_invoice.payment_intent"))
            .build()

        val subscription = runBlocking { service.createSubscription("stripePrice1", "user1") }
        subscription shouldBe stripeSubscription

        coVerify(exactly = 1) { client.createSubscription(matchSubscriptionCreateParams(expectedSubscriptionParams), any()) }
        coVerify(exactly = 0) { client.createCustomer(any(), any()) }
        coVerify(exactly = 0) { userService.getUserById(any()) }
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

    private companion object {
        fun MockKMatcherScope.matchSubscriptionCreateParams(expectedSubscription: SubscriptionCreateParams) = match<SubscriptionCreateParams> {
            assertThat(it).usingRecursiveComparison().isEqualTo(expectedSubscription)
            true
        }


        fun MockKMatcherScope.matchCustomerCreateParams(expectedCustomer: CustomerCreateParams) = match<CustomerCreateParams> {
            assertThat(it).usingRecursiveComparison().isEqualTo(expectedCustomer)
            true
        }    }
}