package com.munoon.heartbeatlive.server.auth

import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.security.core.context.ReactiveSecurityContextHolder

object AuthUtils {
    suspend fun authUser() = ReactiveSecurityContextHolder.getContext()
        .map { it.authentication as CustomJwtAuthenticationToken }
        .awaitSingleOrNull()

    suspend fun authUserId() = authUser()?.userId
}