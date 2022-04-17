package com.munoon.heartbeatlive.server.heartRate

import com.munoon.heartbeatlive.server.StatusResponse
import com.munoon.heartbeatlive.server.auth.AuthUtils
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import reactor.core.publisher.Mono

@Controller
class HeartRateController {
    private val logger = LoggerFactory.getLogger(HeartRateController::class.java)

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    fun addHeartRate(@Argument heartRate: Int): Mono<StatusResponse> {
        return AuthUtils.authenticationPrincipal()
            .doOnNext { logger.info("Received new heart rate: $heartRate [user: ${it.userId}]") }
            .map { StatusResponse(ok = true) }
    }
}