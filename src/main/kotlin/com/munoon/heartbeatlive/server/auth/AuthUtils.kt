package com.munoon.heartbeatlive.server.auth

import org.springframework.security.core.context.ReactiveSecurityContextHolder

object AuthUtils {
    fun authenticationPrincipal() = ReactiveSecurityContextHolder.getContext()
        .map { it.authentication as CustomJwtAuthenticationToken }
}