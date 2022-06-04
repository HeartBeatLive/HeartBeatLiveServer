package com.munoon.heartbeatlive.server.push.controller

import com.munoon.heartbeatlive.server.auth.utils.AuthUtils.authUserId
import com.munoon.heartbeatlive.server.config.properties.OneSignalProperties
import org.apache.commons.codec.binary.Hex
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Controller
class OneSignalIdentifierAuthenticationTokenController(
    private val oneSignalProperties: OneSignalProperties
) {
    private val logger = LoggerFactory.getLogger(OneSignalIdentifierAuthenticationTokenController::class.java)

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    suspend fun getOneSignalIdentifierAuthenticationToken(): String {
        logger.info("User '${authUserId()}' request identifier authentication token for OneSignal")

        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(oneSignalProperties.restApiKey.toByteArray(), "HmacSHA256")
        mac.init(secretKey)
        val token = mac.doFinal(authUserId().toByteArray())
        return Hex.encodeHexString(token)
    }
}