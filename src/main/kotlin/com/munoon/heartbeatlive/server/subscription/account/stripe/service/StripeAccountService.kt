package com.munoon.heartbeatlive.server.subscription.account.stripe.service

import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeAccount
import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeMetadata
import com.munoon.heartbeatlive.server.subscription.account.stripe.client.StripeClient
import com.munoon.heartbeatlive.server.subscription.account.stripe.repository.StripeAccountRepository
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.UserEvents
import com.stripe.param.CustomerCreateParams
import kotlinx.coroutines.runBlocking
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.*

@Service
class StripeAccountService(
    private val accountRepository: StripeAccountRepository,
    private val client: StripeClient,
) {
    suspend fun getRequiredStripeAccount(user: User): StripeAccount {
        accountRepository.findById(user.id)?.let { return it }

        val customerBuilder = CustomerCreateParams.builder()
        customerBuilder.setMetadata(mapOf(
            StripeMetadata.Customer.USER_ID.addValue(user.id)
        ))

        user.email?.takeIf { user.emailVerified }?.let { customerBuilder.setEmail(it) }
        user.displayName?.let { customerBuilder.setName(it) }

        val customer = client.createCustomer(customerBuilder.build(), UUID.randomUUID().toString())
        return accountRepository.save(StripeAccount(id = user.id, stripeAccountId = customer.id))
    }

    suspend fun deleteCustomerByStripeId(stripeCustomerId: String) {
        accountRepository.deleteByStripeAccountId(stripeCustomerId)
    }

    @Async
    @EventListener
    fun handleUserDeletedEvent(event: UserEvents.UserDeletedEvent): Unit = runBlocking {
        val stripeAccount = accountRepository.findById(event.userId) ?: return@runBlocking
        client.deleteCustomer(stripeAccount.stripeAccountId, UUID.randomUUID().toString())
    }
}