package com.munoon.heartbeatlive.server.subscription.account.stripe.service

import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeAccount
import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeCustomerNotFoundByIdException
import com.munoon.heartbeatlive.server.subscription.account.stripe.client.StripeClient
import com.munoon.heartbeatlive.server.subscription.account.stripe.repository.StripeAccountRepository
import com.munoon.heartbeatlive.server.user.User
import com.stripe.model.Event
import com.stripe.model.Subscription
import com.stripe.param.CustomerCreateParams
import com.stripe.param.SubscriptionCreateParams
import com.stripe.param.SubscriptionCreateParams.PaymentSettings.SaveDefaultPaymentMethod
import com.stripe.param.SubscriptionUpdateParams
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.util.*

@Service
class StripeAccountSubscriptionService(
    private val client: StripeClient,
    private val accountRepository: StripeAccountRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    private companion object {
        val SUBSCRIPTION_ELEMENTS_TO_EXPAND = listOf("latest_invoice.payment_intent")
        const val CUSTOMER_USER_ID_METADATA_KEY = "uid"
    }

    suspend fun createSubscription(stripePriceId: String, user: User): Subscription {
        val createSubscriptionParams = SubscriptionCreateParams.builder()
            .setCustomer(getRequiredStripeAccount(user).stripeAccountId)
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

    private suspend fun getRequiredStripeAccount(user: User): StripeAccount {
        accountRepository.findById(user.id)?.let { return it }

        val customerBuilder = CustomerCreateParams.builder()
        customerBuilder.setMetadata(mapOf(CUSTOMER_USER_ID_METADATA_KEY to user.id))
        user.email?.let { customerBuilder.setEmail(it) }
        user.displayName?.let { customerBuilder.setName(it) }

        val customer = client.createCustomer(customerBuilder.build(), UUID.randomUUID().toString())
        return accountRepository.save(StripeAccount(id = user.id, stripeAccountId = customer.id))
    }

    fun handleEvent(event: Event) {
        eventPublisher.publishEvent(event)
    }

    suspend fun getUserIdByStripeCustomerId(customerId: String): String {
        return accountRepository.findByStripeAccountId(customerId)?.id
            ?: throw StripeCustomerNotFoundByIdException(customerId)
    }

    suspend fun deleteCustomerByStripeId(stripeCustomerId: String) {
        accountRepository.deleteByStripeAccountId(stripeCustomerId)
    }

    suspend fun cancelUserSubscription(subscriptionId: String) {
        val subscriptionUpdateParams = SubscriptionUpdateParams.builder().setCancelAtPeriodEnd(true).build()
        client.updateSubscription(subscriptionId, subscriptionUpdateParams, UUID.randomUUID().toString())
    }
}