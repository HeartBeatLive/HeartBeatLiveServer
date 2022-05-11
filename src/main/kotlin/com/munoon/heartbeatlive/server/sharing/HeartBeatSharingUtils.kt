package com.munoon.heartbeatlive.server.sharing

object HeartBeatSharingUtils {
    private val PUBLIC_CODE_ALLOWED_CHARD = ('A'..'Z') + ('0'..'9')
    private val PUBLIC_CODE_LENGTH = 6

    fun generatePublicCode(): String {
        return (1..PUBLIC_CODE_LENGTH)
            .map { PUBLIC_CODE_ALLOWED_CHARD.random() }
            .joinToString("")
    }
}