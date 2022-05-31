package com.munoon.heartbeatlive.server.heartrate.controller

import com.munoon.heartbeatlive.server.auth.utils.AuthUtils.authUserId
import com.munoon.heartbeatlive.server.auth.utils.AuthUtils.authUserMono
import com.munoon.heartbeatlive.server.heartrate.HeartRateMapper.asGraphQL
import com.munoon.heartbeatlive.server.heartrate.model.GraphqlHeartRateInfoTo
import com.munoon.heartbeatlive.server.heartrate.model.GraphqlSendHeartRateInput
import com.munoon.heartbeatlive.server.heartrate.service.HeartRateService
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SubscriptionMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import javax.validation.Valid

@Controller
@PreAuthorize("isAuthenticated()")
class HeartBeatController(private val service: HeartRateService) {
    private val logger = LoggerFactory.getLogger(HeartBeatController::class.java)

    @MutationMapping
    suspend fun sendHeartRate(@Argument @Valid data: GraphqlSendHeartRateInput): Boolean {
        logger.info("Received heart rate from user '${authUserId()}'")
        service.sendHeartRate(authUserId(), data.heartRate)
        return true
    }

    @SubscriptionMapping
    fun subscribeToHeartRates(): Flux<GraphqlHeartRateInfoTo> {
        return authUserMono().flatMapMany { user ->
            logger.info("User '${user.userId}' subscribed to heart rates")
            service.subscribeToHeartRates(user.userId).map { it.asGraphQL() }
        }
    }
}