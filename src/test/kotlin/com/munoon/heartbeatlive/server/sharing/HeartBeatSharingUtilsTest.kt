package com.munoon.heartbeatlive.server.sharing

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class HeartBeatSharingUtilsTest {
    @Test
    fun generatePublicCode() {
        val publicCodes = arrayListOf<String>()
        for (i in 0..100) {
            val publicCode = HeartBeatSharingUtils.generatePublicCode()
            assertThat(publicCode.length).isEqualTo(6)
            assertThat(publicCodes).doesNotContain(publicCode)

            publicCodes += publicCode
        }
    }
}