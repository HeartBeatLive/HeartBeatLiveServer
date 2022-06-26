package com.munoon.heartbeatlive.server.subscription.account.stripe.controller

import com.munoon.heartbeatlive.server.config.properties.StripeConfigurationProperties
import com.munoon.heartbeatlive.server.subscription.account.stripe.service.StripeAccountSubscriptionService
import com.stripe.exception.SignatureVerificationException
import com.stripe.model.Event
import com.stripe.net.ApiResource
import com.stripe.net.Webhook
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/stripe")
class StripeWebhookEventController(
    private val service: StripeAccountSubscriptionService,
    private val stripeConfigurationProperties: StripeConfigurationProperties
) {
    private val logger = LoggerFactory.getLogger(StripeWebhookEventController::class.java)

    @PostMapping("/webhook")
    suspend fun handleWebhookEvent(
        @RequestBody eventJson: String,
        @RequestHeader("Stripe-Signature", required = false) stripeSignature: String?
    ) {
        val event = if (stripeConfigurationProperties.webhookEndpointSecret != null) {
            if (stripeSignature == null) {
                throw SignatureVerificationException("Stripe signature is not provided!", "<null>")
            }
            Webhook.constructEvent(eventJson, stripeSignature, stripeConfigurationProperties.webhookEndpointSecret)
        } else {
            ApiResource.GSON.fromJson(eventJson, Event::class.java)
        }

        logger.info("Received stripe event: ${event.type}")
        service.handleEvent(event)
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(SignatureVerificationException::class)
    fun handleSignatureVerificationException(e: SignatureVerificationException) {
        logger.warn("Signature verification failed for Stripe webhook event", e)
    }
}