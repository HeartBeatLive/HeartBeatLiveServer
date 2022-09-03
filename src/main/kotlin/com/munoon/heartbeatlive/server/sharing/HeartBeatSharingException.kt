package com.munoon.heartbeatlive.server.sharing

import com.munoon.heartbeatlive.server.config.error.ConvertExceptionToError
import org.springframework.graphql.execution.ErrorType

@ConvertExceptionToError(type = ErrorType.NOT_FOUND, code = "heart_beat_sharing.not_found.by_id")
data class HeartBeatSharingNotFoundByIdException(val id: String)
    : RuntimeException("Heart beat sharing with id '$id' is not found!")

@ConvertExceptionToError(type = ErrorType.NOT_FOUND, code = "heart_beat_sharing.not_found.by_public_code")
data class HeartBeatSharingNotFoundByPublicCodeException(val publicCode: String)
    : RuntimeException("Heart beat sharing with public code '$publicCode' is not found!")

@ConvertExceptionToError(type = ErrorType.FORBIDDEN, code = "heart_beat_sharing.limit_exceeded")
data class HeartBeatSharingLimitExceededException(val limit: Int)
    : RuntimeException("User have too many heart beat sharing (limit = $limit).")

@ConvertExceptionToError(type = ErrorType.FORBIDDEN, code = "heart_beat_sharing.expired")
class HeartBeatSharingExpiredException : RuntimeException("Heart beat sharing code has expired!")

@ConvertExceptionToError(type = ErrorType.FORBIDDEN, code = "heart_beat_sharing.locked")
class HeartBeatSharingLockedException : RuntimeException("Heart beat sharing code is locked!")