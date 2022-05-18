package com.munoon.heartbeatlive.server.config.error

import org.springframework.graphql.execution.ErrorType

annotation class ConvertExceptionToError(
    val type: ErrorType,
    val code: String
)
