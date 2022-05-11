package com.munoon.heartbeatlive.server.sharing

data class HeartBeatSharingNotFoundByIdException(val id: String)
    : RuntimeException("Heart beat sharing with id '$id' is not found!")

data class HeartBeatSharingNotFoundByPublicCodeException(val publicCode: String)
    : RuntimeException("Heart beat sharing with public code '$publicCode' is not found!")

data class HeartBeatSharingLimitExceededException(val limit: Int)
    : RuntimeException("User have too many heart beat sharing (limit = $limit).")