@file:Suppress("MatchingDeclarationName")
package com.munoon.heartbeatlive.server.user

data class UserNotFoundByIdException(val id: String)
    : RuntimeException("User with id '$id' is not found!")