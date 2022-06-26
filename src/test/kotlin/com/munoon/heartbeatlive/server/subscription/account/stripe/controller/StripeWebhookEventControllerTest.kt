package com.munoon.heartbeatlive.server.subscription.account.stripe.controller

import com.munoon.heartbeatlive.server.AbstractTest
import com.munoon.heartbeatlive.server.subscription.account.stripe.service.StripeAccountSubscriptionService
import com.ninjasquad.springmockk.MockkBean
import com.stripe.model.Event
import com.stripe.net.Webhook
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Instant

@SpringBootTest(
    properties = ["payment.stripe.webhook-endpoint-secret=${StripeWebhookEventControllerTest.STRIPE_WEBHOOK_SECRET}"]
)
@AutoConfigureWebTestClient
internal class StripeWebhookEventControllerTest : AbstractTest() {
    companion object {
        const val STRIPE_WEBHOOK_SECRET = "stripeWebhookSecret"
    }

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var service: StripeAccountSubscriptionService

    @Test
    fun handleWebhookEvent() {
        val expectedEvent = Event().apply {
            `object` = "event"
            id = "stripe_event_id"
            type = "customer_stripe_event"
        }

        every { service.handleEvent(any()) } returns Unit

        val bodyJson = """{"object":"event","id":"stripe_event_id","type":"customer_stripe_event"}"""

        val time = Instant.now().epochSecond
        val sign = "t=$time,v1=${Webhook.Util.computeHmacSha256(STRIPE_WEBHOOK_SECRET, "$time.$bodyJson")}"

        webTestClient.post()
            .uri("/api/stripe/webhook")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(bodyJson)
            .header("Stripe-Signature", sign)
            .exchange()
            .expectStatus().isOk
            .expectBody().isEmpty

        verify(exactly = 1) { service.handleEvent(expectedEvent) }
    }

    @Test
    fun `handleWebhookEvent - invalid signature`() {
        webTestClient.post()
            .uri("/api/stripe/webhook")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"object":"event","id":"stripe_event_id","type":"customer_stripe_event"}""")
            .header("Stripe-Signature", "abc")
            .exchange()
            .expectStatus().isForbidden
            .expectBody().isEmpty

        verify(exactly = 0) { service.handleEvent(any()) }
    }

    @Test
    fun `handleWebhookEvent - empty signature`() {
        webTestClient.post()
            .uri("/api/stripe/webhook")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"object":"event","id":"stripe_event_id","type":"customer_stripe_event"}""")
            .exchange()
            .expectStatus().isForbidden
            .expectBody().isEmpty

        verify(exactly = 0) { service.handleEvent(any()) }
    }
}