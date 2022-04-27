package com.munoon.heartbeatlive.server.user.firebase

import com.google.api.core.SettableApiFuture
import com.munoon.heartbeatlive.server.user.firebase.FirebaseUtils.await
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

internal class FirebaseUtilsTest {
    @Test
    fun `await on ApiFuture - success`() {
        val future = SettableApiFuture.create<String>().apply { set("OK") }
        val result = runBlocking { future.await() }
        assertThat(result).isEqualTo("OK")
    }

    @Test
    fun `await on ApiFuture - exception`() {
        val future = SettableApiFuture.create<String>().apply { setException(IllegalStateException("Test Exception")) }
        assertThatThrownBy { runBlocking { future.await() } }
            .isExactlyInstanceOf(IllegalStateException::class.java)
    }
}