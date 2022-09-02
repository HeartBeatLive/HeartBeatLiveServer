package com.munoon.heartbeatlive.server.subscription.account.stripe.service

import com.munoon.heartbeatlive.server.AbstractTest
import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeAccount
import com.munoon.heartbeatlive.server.subscription.account.stripe.client.StripeClient
import com.munoon.heartbeatlive.server.subscription.account.stripe.repository.StripeAccountRepository
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.UserEvents
import com.ninjasquad.springmockk.MockkBean
import com.stripe.model.Customer
import com.stripe.param.CustomerCreateParams
import io.kotest.common.runBlocking
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
import org.springframework.context.ApplicationEventPublisher

@SpringBootTest
internal class StripeAccountServiceTest : AbstractTest() {
    @Autowired
    private lateinit var service: StripeAccountService

    @Autowired
    private lateinit var repository: StripeAccountRepository

    @MockkBean
    private lateinit var client: StripeClient

    @Autowired
    private lateinit var eventPubliser: ApplicationEventPublisher

    @Test
    fun `getRequiredStripeAccount - create and return`() {
        val expectedStripeAccount = StripeAccount(id = "user1", stripeAccountId = "stripeCustomer1")
        val expectedCreateCustomerParams = CustomerCreateParams.builder()
            .setMetadata(mapOf("uid" to "user1"))
            .setEmail("email@example.com")
            .setName("Display Name")
            .build()

        coEvery { client.createCustomer(any(), any()) } returns Customer().apply {
            id = "stripeCustomer1"
        }

        val user = User(
            id = "user1",
            displayName = "Display Name",
            email = "email@example.com",
            emailVerified = true
        )

        val result = runBlocking { service.getRequiredStripeAccount(user) }
        result shouldBe expectedStripeAccount

        val allAccounts = runBlocking { repository.findAll().toList(arrayListOf()) }
        assertThat(allAccounts).usingRecursiveComparison().isEqualTo(listOf(expectedStripeAccount))

        coVerify(exactly = 1) { client.createCustomer(
            matchCustomerCreateParams(expectedCreateCustomerParams),
            matchUUID()
        ) }
    }

    @Test
    fun `getRequiredStripeAccount - return existing one`() {
        val expectedStripeAccount = StripeAccount(id = "user1", stripeAccountId = "stripeCustomer1")
        runBlocking { repository.save(expectedStripeAccount) }

        val user = User(
            id = "user1",
            displayName = "Display Name",
            email = "email@example.com",
            emailVerified = true
        )

        val result = runBlocking { service.getRequiredStripeAccount(user) }
        result shouldBe expectedStripeAccount

        val allAccounts = runBlocking { repository.findAll().toList(arrayListOf()) }
        assertThat(allAccounts).usingRecursiveComparison().isEqualTo(listOf(expectedStripeAccount))

        coVerify(exactly = 0) { client.createCustomer(any(), matchUUID()) }
    }

    @Test
    fun deleteCustomerByStripeId(): Unit = runBlocking {
        checkAll(5, Arb.string(), Arb.string()) { userId, stripeCustomerId ->
            repository.save(StripeAccount(id = userId, stripeAccountId = stripeCustomerId))

            service.deleteCustomerByStripeId(stripeCustomerId)

            repository.count() shouldBe 0
        }
    }

    @Test
    fun handleUserDeletedEvent(): Unit = runBlocking {
        coEvery { client.deleteCustomer(any(), any()) } returns Customer()
        repository.save(StripeAccount("user1", "stripeCustomer1"))

        eventPubliser.publishEvent(UserEvents.UserDeletedEvent("user1", false))

        coVerify(exactly = 1) { client.deleteCustomer("stripeCustomer1", matchUUID()) }
    }

    @Test
    fun `handleUserDeletedEvent - ignore`() {
        eventPubliser.publishEvent(UserEvents.UserDeletedEvent("user1", false))

        coVerify(exactly = 0) { client.deleteCustomer(any(), any()) }
    }

    private companion object {
        fun MockKMatcherScope.matchCustomerCreateParams(expectedCustomer: CustomerCreateParams) =
            match<CustomerCreateParams> {
                assertThat(it).usingRecursiveComparison().isEqualTo(expectedCustomer)
                true
            }

        fun MockKMatcherScope.matchUUID() = match<String> {
            it.shouldBeUUID()
            true
        }
    }
}