package com.munoon.heartbeatlive.server.heartRate

import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Mono

@Controller
class HeartRateController {
    private val logger = LoggerFactory.getLogger(HeartRateController::class.java)

    @MutationMapping
    fun addHeartRate(@Argument heartRate: Int): Mono<AddHeartRateResponse> {
        logger.info("Received new heart rate: $heartRate")
        return Mono.just(AddHeartRateResponse(ok = true))
    }
}

data class AddHeartRateResponse(val ok: Boolean)