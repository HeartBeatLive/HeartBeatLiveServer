package com.munoon.heartbeatlive.server.subscription

import com.google.firebase.auth.FirebaseAuth
import com.munoon.heartbeatlive.server.StatusResponse
import com.munoon.heartbeatlive.server.auth.AuthUtils.authenticationPrincipal
import com.munoon.heartbeatlive.server.auth.CustomJwtAuthenticationToken
import com.munoon.heartbeatlive.server.auth.UserSubscriptionPlan
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant

@Controller
class SubscriptionController(private val firebaseAuth: FirebaseAuth) {
    private val logger = LoggerFactory.getLogger(SubscriptionController::class.java)

    @MutationMapping
    fun updateSubscription(@Argument subscriptionPlan: String): Mono<StatusResponse> {
        return authenticationPrincipal()
            .doOnNext { logger.info("Updating subscription for user '${it.name}' to '$subscriptionPlan'") }
            .doOnNext {
                firebaseAuth.setCustomUserClaims(
                    it.userId,
                    mapOf(
                        CustomJwtAuthenticationToken.SUBSCRIPTION_PLAN_CLAIM to JwtUserSubscription(
                            plan = UserSubscriptionPlan.valueOf(subscriptionPlan),
                            expirationTime = Instant.now().plus(Duration.ofMinutes(5))
                        ).asMap()
                    )
                )
            }
            .map { StatusResponse(ok = true) }
    }

    @QueryMapping
    fun getSubscription() = authenticationPrincipal().map { it.actualUserSubscriptionPlan }
}