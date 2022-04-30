package com.munoon.heartbeatlive.server.auth.utils

import com.munoon.heartbeatlive.server.auth.jwt.CustomJwtAuthenticationToken
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.security.core.context.ReactiveSecurityContextHolder

object AuthUtils {
    suspend fun authUser() = ReactiveSecurityContextHolder.getContext()
        .map { it.authentication as CustomJwtAuthenticationToken }
        .awaitSingleOrNull()

    suspend fun optionalAuthUserId(): String? = authUser()?.userId

    suspend fun authUserId(): String = optionalAuthUserId()!!
}