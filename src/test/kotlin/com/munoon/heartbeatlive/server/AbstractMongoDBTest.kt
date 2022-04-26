package com.munoon.heartbeatlive.server

import com.mongodb.BasicDBObject
import com.mongodb.reactivestreams.client.MongoClients
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import reactor.kotlin.core.publisher.toMono

@AutoConfigureDataMongo
@Import(AbstractMongoDBTest.TestMongoConfig::class)
abstract class AbstractMongoDBTest : AbstractTest() {
    @Autowired
    private lateinit var mongoTemplate: ReactiveMongoTemplate

    @TestConfiguration
    class TestMongoConfig {
        @Order(Ordered.HIGHEST_PRECEDENCE)
        @EventListener(ContextRefreshedEvent::class)
        fun createMongoUserIfNotExist() {
            runBlocking {
                val mongoClient = MongoClients.create("mongodb://localhost:27017")
                val adminDb = mongoClient.getDatabase("admin")
                val userName = "mongodb"

                val userExist = adminDb.getCollection("system.users")
                    .countDocuments(BasicDBObject(mapOf("user" to userName)))
                    .awaitSingle() > 0

                if (!userExist) {
                    val createUserCommand = BasicDBObject(mapOf(
                        "createUser" to userName,
                        "pwd" to "password",
                        "roles" to arrayOf("readWrite")
                    ))
                    adminDb.runCommand(createUserCommand).awaitFirstOrNull()
                }
            }
        }
    }

    @BeforeEach
    fun clearDb() {
        mongoTemplate.mongoDatabase.flatMap { it.drop().toMono() }.block()
    }
}