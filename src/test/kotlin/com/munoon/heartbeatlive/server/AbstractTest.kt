package com.munoon.heartbeatlive.server

import com.mongodb.BasicDBObject
import com.mongodb.reactivestreams.client.MongoClients
import com.munoon.heartbeatlive.server.config.MockFirebaseConfiguration
import de.flapdoodle.embed.mongo.MongodExecutable
import de.flapdoodle.embed.mongo.MongodStarter
import de.flapdoodle.embed.mongo.config.MongodConfig
import de.flapdoodle.embed.mongo.config.Net
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.process.runtime.Network
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.test.context.ActiveProfiles
import reactor.kotlin.core.publisher.toMono

@ActiveProfiles("test")
@ExtendWith(AbstractTest.RunMongoExtension::class)
@Import(MockFirebaseConfiguration::class)
abstract class AbstractTest {
    @Autowired
    private lateinit var mongoTemplate: ReactiveMongoTemplate

    class RunMongoExtension : BeforeAllCallback, AfterAllCallback, ExtensionContext.Store.CloseableResource {
        private val logger = LoggerFactory.getLogger(RunMongoExtension::class.java)
        private var mongodExecutable: MongodExecutable? = null

        override fun beforeAll(context: ExtensionContext?) {
            if (mongodExecutable != null) return

            logger.info("Starting MongoDB instance...")
            val mongodConfig = MongodConfig.builder()
                .version(Version.Main.V5_0)
                .net(Net("localhost", 27017, Network.localhostIsIPv6()))
                .build()

            mongodExecutable = MongodStarter.getDefaultInstance().prepare(mongodConfig)
            mongodExecutable!!.start()

            MongoClients.create("mongodb://localhost:27017")
                .getDatabase("admin")
                .runCommand(BasicDBObject(mapOf(
                    "createUser" to "mongodb",
                    "pwd" to "password",
                    "roles" to arrayOf("readWrite")
                )))
                .toMono()
                .block()
        }

        override fun close() {
            shutDownMongo()
        }

        override fun afterAll(context: ExtensionContext?) {
            shutDownMongo()
        }

        private fun shutDownMongo() {
            if (mongodExecutable == null) return
            logger.info("Shutting down MongoDB instance...")
            mongodExecutable!!.stop()
        }
    }

    @BeforeEach
    fun clearDb() {
        mongoTemplate.mongoDatabase.flatMap { it.drop().toMono() }.block()
    }
}