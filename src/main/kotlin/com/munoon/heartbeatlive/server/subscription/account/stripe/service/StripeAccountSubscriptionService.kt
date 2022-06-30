package com.munoon.heartbeatlive.server.subscription.account.stripe.service

import com.munoon.heartbeatlive.server.config.properties.SubscriptionProperties
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeAccount
import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeCustomerNotFoundByIdException
import com.munoon.heartbeatlive.server.subscription.account.stripe.StripeMetadata
import com.munoon.heartbeatlive.server.subscription.account.stripe.client.StripeClient
import com.munoon.heartbeatlive.server.subscription.account.stripe.repository.StripeAccountRepository
import com.munoon.heartbeatlive.server.user.User
import com.stripe.model.Event
import com.stripe.model.Subscription
import com.stripe.param.CustomerCreateParams
import com.stripe.param.RefundCreateParams
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
    }

    suspend fun createSubscription(plan: UserSubscriptionPlan, price: SubscriptionProperties.SubscriptionPrice, user: User): Subscription {
        if (price.stripePriceId == null) {
            throw IllegalArgumentException("Subscription price should include Stripe price id")
        }

        val createSubscriptionParams = SubscriptionCreateParams.builder()
            .setCustomer(getRequiredStripeAccount(user).stripeAccountId)
            .addItem(
                SubscriptionCreateParams.Item.builder()
                    .setPrice(price.stripePriceId)
                    .build()
            )
            .setPaymentSettings(
                SubscriptionCreateParams.PaymentSettings.builder()
                    .setSaveDefaultPaymentMethod(SaveDefaultPaymentMethod.ON_SUBSCRIPTION)
                    .build()
            )
            .putAllMetadata(mapOf(
                StripeMetadata.Subscription.REFUND_DURATION.addValue(price.refundDuration),
                StripeMetadata.Subscription.SUBSCRIPTION_PLAN.addValue(plan),
                StripeMetadata.Subscription.USER_ID.addValue(user.id)
            ))
            .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
            .addAllExpand(SUBSCRIPTION_ELEMENTS_TO_EXPAND)
            .build()

        return client.createSubscription(createSubscriptionParams, UUID.randomUUID().toString())
    }

    private suspend fun getRequiredStripeAccount(user: User): StripeAccount {
        accountRepository.findById(user.id)?.let { return it }

        val customerBuilder = CustomerCreateParams.builder()
        customerBuilder.setMetadata(mapOf(
            StripeMetadata.Customer.USER_ID.addValue(user.id)
        ))

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

    suspend fun makeARefund(subscriptionId: String, paymentIntentId: String, reason: RefundCreateParams.Reason) {
        val refund = RefundCreateParams.builder()
            .setPaymentIntent(paymentIntentId)
            .setReason(reason)
            .build()

        client.createARefund(refund, UUID.randomUUID().toString())
        client.cancelSubscription(subscriptionId, null, UUID.randomUUID().toString())
    }
}