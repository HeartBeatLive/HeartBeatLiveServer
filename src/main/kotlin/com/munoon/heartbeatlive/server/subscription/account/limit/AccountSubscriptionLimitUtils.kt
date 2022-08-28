package com.munoon.heartbeatlive.server.subscription.account.limit

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import org.bson.Document
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.BasicUpdate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo

object AccountSubscriptionLimitUtils {
    suspend fun <T> maintainALimit(
        userId: String,
        newLimit: Int,
        baseSort: Sort,
        repository: AccountSubscriptionLimitRepository<T>
    ) {
        val currentCount = repository.countAllByUserId(userId)
        val currentlyLockedCount = repository.countAllByUserIdAndLockedTrue(userId)
        val currentlyUnlockedCount = currentCount - currentlyLockedCount

        // locking out of limits
        if (currentlyUnlockedCount > newLimit) {
            val shouldBeLockedCount = currentlyUnlockedCount - newLimit
            if (shouldBeLockedCount > 0) {
                val pageRequest = PageRequest.of(0, shouldBeLockedCount, baseSort.ascending())
                val itemsToLock = repository.findIdsByUserId(userId, locked = false, pageRequest)

                repository.lockAllById(itemsToLock, lock = true)
            }
        }

        // unlocking previously locked
        if (currentlyUnlockedCount < newLimit) {
            val canBeUnlockedCount = currentlyLockedCount.coerceAtMost(newLimit - currentlyUnlockedCount)
            if (canBeUnlockedCount > 0) {
                val pageRequest = PageRequest.of(0, canBeUnlockedCount, baseSort.descending())
                val itemsToUnlock = repository.findIdsByUserId(userId, locked = true, pageRequest)

                repository.lockAllById(itemsToUnlock, lock = false)
            }
        }
    }

    suspend inline fun <reified T> ReactiveMongoTemplate.lockAllById(
        lockFieldName: String,
        ids: Set<String>,
        lock: Boolean
    ) {
        val query = Query.query(Criteria.where("_id").inValues(ids))
        val update = BasicUpdate.update(lockFieldName, lock)
        updateMulti(query, update, T::class.java).awaitSingle()
    }

    suspend fun ReactiveMongoTemplate.findIdsByUserId(
        collectionName: String,
        userIdFieldName: String,
        lockedFieldName: String,
        userId: String,
        locked: Boolean,
        pageable: Pageable
    ): Set<String> {
        val criteria = Criteria.where(userIdFieldName).isEqualTo(userId)
            .and(lockedFieldName).isEqualTo(locked)

        val query = Query.query(criteria)
            .with(pageable)
            .apply { fields().include("_id") }

        return find(query, Document::class.java, collectionName)
            .asFlow()
            .map { it.getObjectId("_id").toHexString() }
            .toSet(hashSetOf())
    }
}

interface AccountSubscriptionLimitRepository<I> {
    suspend fun countAllByUserId(userId: String): Int

    suspend fun countAllByUserIdAndLockedTrue(userId: String): Int

    suspend fun findIdsByUserId(userId: String, locked: Boolean, pageable: Pageable): Set<I>

    suspend fun lockAllById(ids: Set<I>, lock: Boolean)
}