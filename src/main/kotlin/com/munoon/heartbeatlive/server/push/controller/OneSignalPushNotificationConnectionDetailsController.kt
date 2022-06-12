package com.munoon.heartbeatlive.server.push.controller

import com.munoon.heartbeatlive.server.auth.utils.AuthUtils.authUserId
import com.munoon.heartbeatlive.server.config.properties.OneSignalProperties
import com.munoon.heartbeatlive.server.push.model.GraphqlOneSignalPushNotificationConnectionDetails
import org.apache.commons.codec.binary.Hex
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Controller
class OneSignalPushNotificationConnectionDetailsController(
    private val oneSignalProperties: OneSignalProperties
) {
    private val logger = LoggerFactory.getLogger(OneSignalPushNotificationConnectionDetailsController::class.java)
    private val connectionDetails = GraphqlOneSignalPushNotificationConnectionDetails(oneSignalProperties.appId)

    @QueryMapping
    fun getOneSignalPushNotificationConnectionDetails() = connectionDetails

    @PreAuthorize("isAuthenticated()")
    @SchemaMapping(typeName = "OneSignalPushNotificationConnectionDetails", field = "identifierAuthenticationToken")
    suspend fun getOneSignalIdentifierAuthenticationToken(): String {
        logger.info("User '${authUserId()}' request identifier authentication token for OneSignal")

        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(oneSignalProperties.restApiKey.toByteArray(), "HmacSHA256")
        mac.init(secretKey)
        val token = mac.doFinal(authUserId().toByteArray())
        return Hex.encodeHexString(token)
    }
}