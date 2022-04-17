package com.munoon.heartbeatlive.server.auth

import org.springframework.security.core.GrantedAuthority

enum class UserRole : GrantedAuthority {
    ADMIN;

    override fun getAuthority() = name
}