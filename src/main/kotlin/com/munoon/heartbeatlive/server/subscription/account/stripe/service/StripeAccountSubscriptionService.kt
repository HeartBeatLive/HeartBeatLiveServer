package com.munoon.heartbeatlive.server.subscription.account.stripe.service

import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeAccount
import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeCustomerNotFoundByIdException
import com.munoon.heartbeatlive.server.subscription.account.stripe.client.StripeClient
import com.munoon.heartbeatlive.server.subscription.account.stripe.repository.StripeAccountRepository
import com.munoon.heartbeatlive.server.user.service.UserService
import com.stripe.model.Event
import com.stripe.model.Subscription
import com.stripe.param.CustomerCreateParams
import com.stripe.param.SubscriptionCreateParams
import com.stripe.param.SubscriptionCreateParams.PaymentSettings.SaveDefaultPaymentMethod
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.util.*

@Service
class StripeAccountSubscriptionService(
    private val client: StripeClient,
    private val accountRepository: StripeAccountRepository,
    private val userService: UserService,
    private val eventPublisher: ApplicationEventPublisher
) {
    private companion object {
        val SUBSCRIPTION_ELEMENTS_TO_EXPAND = listOf("latest_invoice.payment_intent")
    }

    suspend fun createSubscription(stripePriceId: String, userId: String): Subscription {
        val createSubscriptionParams = SubscriptionCreateParams.builder()
            .setCustomer(getRequiredStripeAccount(userId).stripeAccountId)
            .addItem(
                SubscriptionCreateParams.Item.builder()
                    .setPrice(stripePriceId)
                    .build()
            )
            .setPaymentSettings(
                SubscriptionCreateParams.PaymentSettings.builder()
                    .setSaveDefaultPaymentMethod(SaveDefaultPaymentMethod.ON_SUBSCRIPTION)
                    .build()
            )
            .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
            .addAllExpand(SUBSCRIPTION_ELEMENTS_TO_EXPAND)
            .build()

        return client.createSubscription(createSubscriptionParams, UUID.randomUUID().toString())
    }

    private suspend fun getRequiredStripeAccount(userId: String): StripeAccount {
        accountRepository.findById(userId)?.let { return it }

        val user = userService.getUserById(userId)

        val customerBuilder = CustomerCreateParams.builder()
        user.email?.let { customerBuilder.setEmail(it) }
        user.displayName?.let { customerBuilder.setName(it) }

        val customer = client.createCustomer(customerBuilder.build(), UUID.randomUUID().toString())
        return accountRepository.save(StripeAccount(id = userId, stripeAccountId = customer.id))
    }

    fun handleEvent(event: Event) {
        eventPublisher.publishEvent(event)
    }

    suspend fun getUserIdByStripeCustomerId(customerId: String): String {
        return accountRepository.findByStripeAccountId(customerId)?.id
            ?: throw StripeCustomerNotFoundByIdException(customerId)
    }
}