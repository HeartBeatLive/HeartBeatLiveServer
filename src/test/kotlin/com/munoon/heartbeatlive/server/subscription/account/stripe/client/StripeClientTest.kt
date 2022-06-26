package com.munoon.heartbeatlive.server.subscription.account.stripe.client

import com.munoon.heartbeatlive.server.config.properties.StripeConfigurationProperties
import com.stripe.exception.ApiConnectionException
import com.stripe.model.Customer
import com.stripe.model.Price
import com.stripe.model.Subscription
import com.stripe.model.SubscriptionItem
import com.stripe.model.SubscriptionItemCollection
import com.stripe.param.CustomerCreateParams
import com.stripe.param.SubscriptionCreateParams
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.reactor.mono
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.FormHttpMessageWriter
import org.springframework.http.codec.HttpMessageWriter
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.mock.http.client.reactive.MockClientHttpRequest
import org.springframework.web.reactive.function.BodyInserter
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import java.math.BigDecimal
import java.net.URLDecoder
import java.util.*

internal class StripeClientTest : FreeSpec({
    val properties = StripeConfigurationProperties().apply {
        privateApiKey = "StripePrivateApiKey"
    }

    "createCustomer" - {
        "successful" {
            val webClientExchangeFunction = mockk<ExchangeFunction>()
            val webClient = WebClient.builder()
                .exchangeFunction(webClientExchangeFunction)
                .build()

            val client = StripeClient(webClient, properties)

            every { webClientExchangeFunction.exchange(any()) } returns mono {
                ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("""
                        {
                          "id": "cus_9utnxg47pWjV1e",
                          "object": "customer",
                          "address": null,
                          "balance": 10,
                          "created": 1484209932,
                          "currency": "usd",
                          "default_source": "card_1LDXuH2eZvKYlo2CnGTzCC8s",
                          "delinquent": false,
                          "description": null,
                          "discount": null,
                          "email": null,
                          "invoice_prefix": "DBFFDB8",
                          "invoice_settings": {
                            "custom_fields": null,
                            "default_payment_method": null,
                            "footer": null,
                            "rendering_options": null
                          },
                          "livemode": false,
                          "metadata": {},
                          "name": null,
                          "next_invoice_sequence": 23091,
                          "phone": null,
                          "preferred_locales": [],
                          "shipping": null,
                          "tax_exempt": "none",
                          "test_clock": null
                        }
                    """.trimIndent())
                    .build()
            }

            val expected = Customer().apply {
                id = "cus_9utnxg47pWjV1e"
                `object` = "customer"
                balance = 10
                created = 1484209932
                currency = "usd"
                defaultSource = "card_1LDXuH2eZvKYlo2CnGTzCC8s"
                delinquent = false
                invoicePrefix = "DBFFDB8"
                livemode = false
                nextInvoiceSequence = 23091
                invoiceSettings = Customer.InvoiceSettings()
                metadata = emptyMap()
                preferredLocales = emptyList()
                taxExempt = "none"
            }

            val expectedFormData = mapOf(
                "name" to "Customer Name",
                "metadata[key_1]" to "value_1",
                "metadata[key_2]" to "value_2",
                "expand[0]" to "expand_1",
                "expand[1]" to "expand_2",
                "expand[2]" to "expand_3"
            )

            val customerCreateParams = CustomerCreateParams.builder()
                .setName("Customer Name")
                .putMetadata("key_1", "value_1")
                .putMetadata("key_2", "value_2")
                .addExpand("expand_1")
                .addExpand("expand_2")
                .addExpand("expand_3")
                .build()

            val idempotentKey = UUID.randomUUID().toString()
            client.createCustomer(customerCreateParams, idempotentKey) shouldBe expected

            verify(exactly = 1) { webClientExchangeFunction.exchange(match {
                it.headers().accept.first() shouldBe MediaType.APPLICATION_JSON
                it.headers().contentType shouldBe MediaType.APPLICATION_FORM_URLENCODED
                it.headers()[HttpHeaders.AUTHORIZATION]?.first() shouldBe "Bearer StripePrivateApiKey"
                it.headers()["Idempotency-Key"]?.first() shouldBe idempotentKey
                it.url().toString() shouldBe "https://api.stripe.com/v1/customers"
                it.method() shouldBe HttpMethod.POST

                val request = MockClientHttpRequest(HttpMethod.POST, "https://api.stripe.com/v1/customers")
                it.body().insert(request, object : BodyInserter.Context {
                    val hints = mutableMapOf<String, Any>()
                    override fun messageWriters(): MutableList<HttpMessageWriter<*>> = mutableListOf(
                        FormHttpMessageWriter()
                    )
                    override fun serverRequest() = Optional.empty<ServerHttpRequest>()
                    override fun hints(): MutableMap<String, Any> = hints
                }).block()

                val form = request.bodyAsString.block().let { form -> URLDecoder.decode(form, Charsets.UTF_8) }
                    .split("&")
                    .toList()
                    .associate { formItem -> formItem.split("=")[0] to formItem.split("=")[1] }
                form shouldBe expectedFormData

                true
            }) }
        }

        "unsuccessful response status" {
            val webClientExchangeFunction = mockk<ExchangeFunction>()
            val webClient = WebClient.builder()
                .exchangeFunction(webClientExchangeFunction)
                .build()

            val client = StripeClient(webClient, properties)

            every { webClientExchangeFunction.exchange(any()) } returns mono {
                ClientResponse.create(HttpStatus.FORBIDDEN)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("{}")
                    .build()
            }

            shouldThrowExactly<ApiConnectionException> {
                client.createCustomer(CustomerCreateParams.builder().build(), UUID.randomUUID().toString())
            }
        }
    }

    "createSubscription" - {
        "successful" {
            val webClientExchangeFunction = mockk<ExchangeFunction>()
            val webClient = WebClient.builder()
                .exchangeFunction(webClientExchangeFunction)
                .build()

            val client = StripeClient(webClient, properties)

            every { webClientExchangeFunction.exchange(any()) } returns mono {
                ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("""
                        {
                          "id": "sub_1JbjFX2eZvKYlo2CQdChmkpk",
                          "object": "subscription",
                          "application": null,
                          "application_fee_percent": null,
                          "automatic_tax": {
                            "enabled": false
                          },
                          "billing_cycle_anchor": 1633341442,
                          "billing_thresholds": null,
                          "cancel_at": null,
                          "cancel_at_period_end": false,
                          "canceled_at": null,
                          "collection_method": "charge_automatically",
                          "created": 1632131842,
                          "current_period_end": 1656928642,
                          "current_period_start": 1655114242,
                          "customer": "cus_KGFlKW5JEeYpeP",
                          "days_until_due": null,
                          "default_payment_method": "pm_1JbjFU2eZvKYlo2CDqHLZFWU",
                          "default_source": null,
                          "default_tax_rates": [],
                          "description": null,
                          "discount": null,
                          "ended_at": null,
                          "items": {
                            "object": "list",
                            "data": [
                              {
                                "id": "si_KGFl0hdv6P9Yww",
                                "object": "subscription_item",
                                "billing_thresholds": null,
                                "created": 1632131843,
                                "metadata": {},
                                "price": {
                                  "id": "15",
                                  "object": "price",
                                  "active": true,
                                  "billing_scheme": "per_unit",
                                  "created": 1386685951,
                                  "currency": "usd",
                                  "custom_unit_amount": null,
                                  "livemode": false,
                                  "lookup_key": null,
                                  "metadata": {
                                    "charset": "utf-8",
                                    "content": "15"
                                  },
                                  "nickname": null,
                                  "product": "prod_BTdpcRLIUTfsFR",
                                  "recurring": {
                                    "aggregate_usage": null,
                                    "interval": "week",
                                    "interval_count": 3,
                                    "usage_type": "licensed"
                                  },
                                  "tax_behavior": "unspecified",
                                  "tiers_mode": null,
                                  "transform_quantity": null,
                                  "type": "recurring",
                                  "unit_amount": 444,
                                  "unit_amount_decimal": "444"
                                },
                                "quantity": 1,
                                "subscription": "sub_1JbjFX2eZvKYlo2CQdChmkpk",
                                "tax_rates": []
                              }
                            ],
                            "has_more": false,
                            "url": "/v1/subscription_items?subscription=sub_1JbjFX2eZvKYlo2CQdChmkpk"
                          },
                          "latest_invoice": "in_1LAA3N2eZvKYlo2CUwGGZTlC",
                          "livemode": false,
                          "metadata": {},
                          "next_pending_invoice_item_invoice": null,
                          "pause_collection": null,
                          "payment_settings": {
                            "payment_method_options": null,
                            "payment_method_types": null,
                            "save_default_payment_method": null
                          },
                          "pending_invoice_item_interval": null,
                          "pending_setup_intent": null,
                          "pending_update": null,
                          "schedule": null,
                          "start_date": 1632131842,
                          "status": "active",
                          "test_clock": null,
                          "transfer_data": null,
                          "trial_end": 1633341442,
                          "trial_start": 1632131842
                        }
                    """.trimIndent())
                    .build()
            }

            val expectedFormData = mapOf(
                "expand[0]" to "expand_1",
                "expand[1]" to "expand_2"
            )

            val expectedSubscription = Subscription().apply {
                id = "sub_1JbjFX2eZvKYlo2CQdChmkpk"
                `object` = "subscription"
                automaticTax = Subscription.AutomaticTax().apply { enabled = false }
                billingCycleAnchor = 1633341442
                cancelAtPeriodEnd = false
                collectionMethod = "charge_automatically"
                created = 1632131842
                currentPeriodStart = 1655114242
                currentPeriodEnd = 1656928642
                customer = "cus_KGFlKW5JEeYpeP"
                defaultPaymentMethod = "pm_1JbjFU2eZvKYlo2CDqHLZFWU"
                defaultTaxRates = emptyList()
                startDate = 1632131842
                status = "active"
                trialStart = 1632131842
                trialEnd = 1633341442
                paymentSettings = Subscription.PaymentSettings()
                latestInvoice = "in_1LAA3N2eZvKYlo2CUwGGZTlC"
                livemode = false
                metadata = emptyMap()
                items = SubscriptionItemCollection().apply {
                    `object` = "list"
                    data = listOf(
                        SubscriptionItem().apply {
                            id = "si_KGFl0hdv6P9Yww"
                            `object` = "subscription_item"
                            created = 1632131843
                            metadata = emptyMap()
                            quantity = 1
                            subscription = "sub_1JbjFX2eZvKYlo2CQdChmkpk"
                            taxRates = emptyList()
                            price = Price().apply {
                                id = "15"
                                `object` = "price"
                                active = true
                                billingScheme = "per_unit"
                                created = 1386685951
                                currency = "usd"
                                livemode = false
                                metadata = mapOf("charset" to "utf-8", "content" to "15")
                                product = "prod_BTdpcRLIUTfsFR"
                                recurring = Price.Recurring().apply {
                                    interval = "week"
                                    intervalCount = 3
                                    usageType = "licensed"
                                }
                                taxBehavior = "unspecified"
                                type = "recurring"
                                unitAmount = 444
                                unitAmountDecimal = BigDecimal(444)
                            }
                        }
                    )
                    hasMore = false
                    url = "/v1/subscription_items?subscription=sub_1JbjFX2eZvKYlo2CQdChmkpk"
                }
            }

            val subscription = SubscriptionCreateParams.builder()
                .addExpand("expand_1")
                .addExpand("expand_2")
                .build()

            val idempotentKey = UUID.randomUUID().toString()
            client.createSubscription(subscription, idempotentKey) shouldBe expectedSubscription

            verify(exactly = 1) { webClientExchangeFunction.exchange(match {
                it.headers().accept.first() shouldBe MediaType.APPLICATION_JSON
                it.headers().contentType shouldBe MediaType.APPLICATION_FORM_URLENCODED
                it.headers()[HttpHeaders.AUTHORIZATION]?.first() shouldBe "Bearer StripePrivateApiKey"
                it.headers()["Idempotency-Key"]?.first() shouldBe idempotentKey
                it.url().toString() shouldBe "https://api.stripe.com/v1/subscriptions"
                it.method() shouldBe HttpMethod.POST

                val request = MockClientHttpRequest(HttpMethod.POST, "https://api.stripe.com/v1/subscriptions")
                it.body().insert(request, object : BodyInserter.Context {
                    val hints = mutableMapOf<String, Any>()
                    override fun messageWriters(): MutableList<HttpMessageWriter<*>> = mutableListOf(
                        FormHttpMessageWriter()
                    )
                    override fun serverRequest() = Optional.empty<ServerHttpRequest>()
                    override fun hints(): MutableMap<String, Any> = hints
                }).block()

                val form = request.bodyAsString.block().let { form -> URLDecoder.decode(form, Charsets.UTF_8) }
                    .split("&")
                    .toList()
                    .associate { formItem -> formItem.split("=")[0] to formItem.split("=")[1] }
                form shouldBe expectedFormData

                true
            }) }
        }

        "unsuccessful response status" {
            val webClientExchangeFunction = mockk<ExchangeFunction>()
            val webClient = WebClient.builder()
                .exchangeFunction(webClientExchangeFunction)
                .build()

            val client = StripeClient(webClient, properties)

            every { webClientExchangeFunction.exchange(any()) } returns mono {
                ClientResponse.create(HttpStatus.FORBIDDEN)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("{}")
                    .build()
            }

            shouldThrowExactly<ApiConnectionException> {
                client.createSubscription(SubscriptionCreateParams.builder().build(), UUID.randomUUID().toString())
            }
        }
    }
})