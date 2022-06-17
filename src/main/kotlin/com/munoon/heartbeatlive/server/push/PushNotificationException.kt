@file:Suppress("MatchingDeclarationName")
package com.munoon.heartbeatlive.server.push

import com.munoon.heartbeatlive.server.config.error.ConvertExceptionToError
import org.springframework.graphql.execution.ErrorType

@ConvertExceptionToError(type = ErrorType.NOT_FOUND, code = "push_notification.not_found.by_id")
data class PushNotificationNotFoundByIdException(val id: String)
    : RuntimeException("Push notification with id '$id' is not found!")