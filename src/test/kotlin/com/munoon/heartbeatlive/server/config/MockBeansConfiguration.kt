package com.munoon.heartbeatlive.server.config

import com.google.firebase.auth.FirebaseAuth
import com.munoon.heartbeatlive.server.onesignal.OneSignalClient
import io.mockk.coEvery
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class MockBeansConfiguration {
    @Bean
    @Primary
    fun firebaseAuth(): FirebaseAuth = mockk(relaxed = true)

    @Bean
    @Primary
    fun mockOneSignalClient(): OneSignalClient = mockk() {
        coEvery { sendNotification(any()) } returns Unit
    }
}