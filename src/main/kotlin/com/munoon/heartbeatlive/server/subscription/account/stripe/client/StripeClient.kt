package com.munoon.heartbeatlive.server.subscription.account.stripe.client

import com.munoon.heartbeatlive.server.config.properties.StripeConfigurationProperties
import com.stripe.exception.ApiConnectionException
import com.stripe.model.Customer
import com.stripe.model.Invoice
import com.stripe.model.Refund
import com.stripe.model.Subscription
import com.stripe.net.ApiRequestParams
import com.stripe.net.ApiResource
import com.stripe.net.FormEncoder
import com.stripe.param.CustomerCreateParams
import com.stripe.param.InvoiceRetrieveParams
import com.stripe.param.RefundCreateParams
import com.stripe.param.SubscriptionCancelParams
import com.stripe.param.SubscriptionCreateParams
import com.stripe.param.SubscriptionUpdateParams
import kotlinx.coroutines.reactor.mono
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.MultiValueMapAdapter
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.util.UriComponentsBuilder
import java.util.stream.Collectors

@Component
class StripeClient(
    private val webClient: WebClient,
    private val properties: StripeConfigurationProperties
) {
    private companion object {
        const val STRIPE_API_BASE_URL = "https://api.stripe.com"
        const val IDEMPOTENCY_KEY_HEADER = "Idempotency-Key"
        const val CUSTOMERS_PATH = "/v1/customers"
        const val SUBSCRIPTIONS_PATH = "/v1/subscriptions"
        const val REFUNDS_PATH = "/v1/refunds"
        const val INVOICE_PATH = "/v1/invoices"
    }

    suspend fun createCustomer(customer: CustomerCreateParams, idempotentKey: String): Customer {
        return makeStripeRequest(CUSTOMERS_PATH, HttpMethod.POST, customer, idempotentKey)
    }

    suspend fun createSubscription(subscription: SubscriptionCreateParams, idempotentKey: String): Subscription {
        return makeStripeRequest(SUBSCRIPTIONS_PATH, HttpMethod.POST, subscription, idempotentKey)
    }

    suspend fun updateSubscription(subscriptionId: String, subscription: SubscriptionUpdateParams, idempotentKey: String): Subscription {
        val uriSuffix = SUBSCRIPTIONS_PATH + "/" + ApiResource.urlEncodeId(subscriptionId)
        return makeStripeRequest(uriSuffix, HttpMethod.POST, subscription, idempotentKey)
    }

    suspend fun cancelSubscription(subscriptionId: String, params: SubscriptionCancelParams?, idempotentKey: String): Subscription {
        val uriSuffix = SUBSCRIPTIONS_PATH + "/" + ApiResource.urlEncodeId(subscriptionId)
        return makeStripeRequest(uriSuffix, HttpMethod.DELETE, params, idempotentKey)
    }

    suspend fun createARefund(refund: RefundCreateParams, idempotentKey: String): Refund {
        return makeStripeRequest(REFUNDS_PATH, HttpMethod.POST, refund, idempotentKey)
    }

    suspend fun getInvoice(invoiceId: String, idempotentKey: String, vararg expandFields: String): Invoice {
        val uriSuffix = INVOICE_PATH + "/" + ApiResource.urlEncodeId(invoiceId)
        val requestParams = InvoiceRetrieveParams.builder().addAllExpand(expandFields.toList()).build()
        return makeStripeRequest(uriSuffix, HttpMethod.GET, requestParams, idempotentKey)
    }

    private suspend inline fun <reified T> makeStripeRequest(
        uriSuffix: String,
        method: HttpMethod,
        data: ApiRequestParams?,
        idempotentKey: String
    ): T {
        val formData = FormEncoder.flattenParams(data?.toMap() ?: emptyMap())
            .stream()
            .collect(Collectors.toMap({ it.key }, { listOf(it.value.toString()) }))
            .let { MultiValueMapAdapter(it) }

        val uri = UriComponentsBuilder.fromUriString(STRIPE_API_BASE_URL + uriSuffix)
        if (method == HttpMethod.GET) {
            uri.queryParams(formData)
        }

        return webClient.method(method).uri(uri.build().toUriString())
            .addHeaders(idempotentKey)
            .also {
                if (method != HttpMethod.GET) {
                    it.body(BodyInserters.fromFormData(formData))
                }
            }
            .retrieve()
            .onStatus({ !it.is2xxSuccessful }) { mono {
                ApiConnectionException("Response status is ${it.statusCode()}: ${it.awaitBody<String>()}")
            } }
            .awaitBody<String>()
            .let { json -> ApiResource.GSON.fromJson(json, T::class.java) }
    }

    private fun WebClient.RequestBodySpec.addHeaders(idempotentKey: String): WebClient.RequestBodySpec {
        return contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .accept(MediaType.APPLICATION_JSON)
            .header(IDEMPOTENCY_KEY_HEADER, idempotentKey)
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${properties.privateApiKey}")
    }
}