package com.munoon.heartbeatlive.server.config

import com.google.firebase.auth.FirebaseAuth
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class MockFirebaseConfiguration {
    @Bean
    @Primary
    fun firebaseAuth(): FirebaseAuth = mockk(relaxed = true)
}