package com.munoon.heartbeatlive.server

import com.munoon.heartbeatlive.server.config.MockFirebaseConfiguration
import org.cache2k.CacheManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.wait.strategy.Wait
import reactor.kotlin.core.publisher.toMono
import java.io.File

@ActiveProfiles("test")
@ExtendWith(AbstractTest.RunContainers::class)
@Import(MockFirebaseConfiguration::class)
abstract class AbstractTest {
    @Autowired
    private lateinit var mongoTemplate: ReactiveMongoTemplate

    @Autowired
    private lateinit var redisConnectionFactory: ReactiveRedisConnectionFactory

    class RunContainers : BeforeAllCallback, AfterAllCallback, ExtensionContext.Store.CloseableResource {
        private val containers = DockerComposeContainer(File("docker-compose.yml"))
            .withExposedService("mongo_1", 27017, Wait.forHealthcheck())
            .withExposedService("redis_1", 6379, Wait.forHealthcheck())

        override fun beforeAll(context: ExtensionContext?) {
            containers.start()
        }

        override fun close() {
            containers.stop()
        }

        override fun afterAll(context: ExtensionContext?) {
            containers.stop()
        }
    }

    @BeforeEach
    fun clearDb() {
        mongoTemplate.mongoDatabase.flatMap { it.drop().toMono() }.block()
        ReactiveStringRedisTemplate(redisConnectionFactory)
            .execute { it.serverCommands().flushAll() }
            .blockLast()
    }

    @AfterEach
    fun clearCaches() {
        CacheManager.closeAll()
    }
}