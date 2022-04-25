package com.munoon.heartbeatlive.server

import com.munoon.heartbeatlive.server.config.MockFirebaseConfiguration
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@Import(MockFirebaseConfiguration::class)
abstract class AbstractTest {
}