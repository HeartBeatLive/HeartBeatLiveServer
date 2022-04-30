package com.munoon.heartbeatlive.server.user

import org.springframework.security.core.GrantedAuthority

enum class UserRole : GrantedAuthority {
    ADMIN;

    override fun getAuthority() = name
}