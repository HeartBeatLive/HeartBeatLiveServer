package com.munoon.heartbeatlive.server.config

import com.munoon.heartbeatlive.server.user.User
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.indexOps

@Configuration
class MongoConfig(private val mongoTemplate: ReactiveMongoTemplate) {
    @EventListener(ContextRefreshedEvent::class)
    fun createMongoIndexes() {
        mongoTemplate.indexOps<User>()
            .ensureIndex(Index("email", Sort.Direction.ASC).unique().sparse().named(User.UNIQUE_EMAIL_INDEX))
            .block()
    }
}