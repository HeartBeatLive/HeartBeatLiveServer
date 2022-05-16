package com.munoon.heartbeatlive.server.config

import com.munoon.heartbeatlive.server.ban.UserBan
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharing
import com.munoon.heartbeatlive.server.subscription.Subscription
import com.munoon.heartbeatlive.server.user.User
import org.bson.Document
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.indexOps

@Configuration
class MongoConfig(private val mongoTemplate: ReactiveMongoTemplate) {
    @EventListener(ContextRefreshedEvent::class)
    fun createMongoIndexes() {
        mongoTemplate.indexOps<User>()
            .ensureIndex(Index("email", Sort.Direction.ASC).unique().sparse().named(User.UNIQUE_EMAIL_INDEX))
            .block()

        mongoTemplate.indexOps<HeartBeatSharing>()
            .ensureIndex(Index("publicCode", Sort.Direction.ASC).unique().named(HeartBeatSharing.UNIQUE_PUBLIC_CODE_INDEX))
            .block()

        mongoTemplate.indexOps<Subscription>()
            .ensureIndex(
                CompoundIndexDefinition(Document(mapOf("userId" to 1, "subscriberUserId" to 1)))
                    .unique()
                    .named(Subscription.UNIQUE_USER_ID_AND_SUBSCRIBER_USER_ID_INDEX)
            )
            .block()

        mongoTemplate.indexOps<UserBan>()
            .ensureIndex(
                CompoundIndexDefinition(Document(mapOf("userId" to 1, "bannedUserId" to 1)))
                    .unique()
                    .named(UserBan.UNIQUE_USER_ID_AND_BANNED_USER_ID_INDEX)
            )
            .block()
    }
}