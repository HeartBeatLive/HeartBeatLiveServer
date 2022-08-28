package com.munoon.heartbeatlive.server.subscription.account.limit

import com.mongodb.client.result.UpdateResult
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharing
import com.munoon.heartbeatlive.server.subscription.account.limit.AccountSubscriptionLimitUtils.findIdsByUserId
import com.munoon.heartbeatlive.server.subscription.account.limit.AccountSubscriptionLimitUtils.lockAllById
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bson.Document
import org.bson.types.ObjectId
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.BasicUpdate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import reactor.core.publisher.Flux
import reactor.kotlin.core.publisher.toMono

internal class AccountSubscriptionLimitUtilsTest : FreeSpec({
    "maintainALimit" - {
        "usage example" - {
            data class Item(val id: Int, val userId: String, val createdDay: Int, val locked: Boolean)
            class SampleRepository(startItems: List<Item>) : AccountSubscriptionLimitRepository<Int> {
                private val items = startItems.toMutableList()

                override suspend fun countAllByUserId(userId: String) =
                    items.filter { it.userId == userId }.size

                override suspend fun countAllByUserIdAndLockedTrue(userId: String) =
                    items.filter { it.userId == userId && it.locked }.size

                override suspend fun findIdsByUserId(
                    userId: String,
                    locked: Boolean,
                    pageable: Pageable,
                ): Set<Int> {
                    return items.filter { it.userId == userId && it.locked == locked }
                        .let {
                            if (pageable.sort.iterator().next().isAscending) it.sortedBy(Item::createdDay)
                            else it.sortedByDescending(Item::createdDay)
                        }
                        .take(pageable.pageSize)
                        .map { it.id }
                        .toSet()
                }

                override suspend fun lockAllById(ids: Set<Int>, lock: Boolean) {
                    for (id in ids) {
                        val index = items.indexOfFirst { it.id == id }
                        items[index] = items[index].copy(locked = lock)
                    }
                }

                fun getAll() = items.toList()
            }

            "lock" {
                val expected = listOf(
                    Item(id = 1, userId = "user1", createdDay = 1, locked = true),
                    Item(id = 2, userId = "user1", createdDay = 2, locked = true),
                    Item(id = 3, userId = "user1", createdDay = 3, locked = true), // locking the oldest x
                    Item(id = 4, userId = "user1", createdDay = 4, locked = false),
                    Item(id = 5, userId = "user2", createdDay = 5, locked = false)
                )

                val repository = SampleRepository(listOf(
                    Item(id = 1, userId = "user1", createdDay = 1, locked = true),
                    Item(id = 2, userId = "user1", createdDay = 2, locked = false),
                    Item(id = 3, userId = "user1", createdDay = 3, locked = false),
                    Item(id = 4, userId = "user1", createdDay = 4, locked = false),
                    Item(id = 5, userId = "user2", createdDay = 5, locked = false)
                ))

                AccountSubscriptionLimitUtils.maintainALimit(
                    userId = "user1",
                    newLimit = 1,
                    baseSort = Sort.by("createdDay"),
                    repository = repository
                )

                repository.getAll() shouldBe expected
            }

            "unlock" {
                val expected = listOf(
                    Item(id = 1, userId = "user1", createdDay = 1, locked = true),
                    Item(id = 2, userId = "user1", createdDay = 2, locked = false),
                    Item(id = 3, userId = "user1", createdDay = 3, locked = false), // unlocking the newest x
                    Item(id = 4, userId = "user1", createdDay = 4, locked = false),
                    Item(id = 5, userId = "user2", createdDay = 5, locked = false)
                )

                val repository = SampleRepository(listOf(
                    Item(id = 1, userId = "user1", createdDay = 1, locked = true),
                    Item(id = 2, userId = "user1", createdDay = 2, locked = true),
                    Item(id = 3, userId = "user1", createdDay = 3, locked = true),
                    Item(id = 4, userId = "user1", createdDay = 4, locked = false),
                    Item(id = 5, userId = "user2", createdDay = 5, locked = false)
                ))

                AccountSubscriptionLimitUtils.maintainALimit(
                    userId = "user1",
                    newLimit = 3,
                    baseSort = Sort.by("createdDay"),
                    repository = repository
                )

                repository.getAll() shouldBe expected
            }
        }

        "lock some" {
            // new limit is 5
            // user have 9 unlocked and 1 locked
            // should lock 4

            val repository = mockk<AccountSubscriptionLimitRepository<String>>() {
                coEvery { countAllByUserId(any()) } returns 10
                coEvery { findIdsByUserId(any(), any(), any()) } returns setOf("a", "b", "c")
                coEvery { lockAllById(any(), any()) } returns Unit
                coEvery { countAllByUserIdAndLockedTrue(any()) } returns 1
            }

            AccountSubscriptionLimitUtils.maintainALimit(
                userId = "user1",
                newLimit = 5,
                baseSort = Sort.by("created"),
                repository = repository
            )

            val expectedPageRequest = PageRequest.of(0, 4, Sort.by("created").ascending())
            coVerify(exactly = 1) { repository.countAllByUserId("user1") }
            coVerify(exactly = 1) { repository.findIdsByUserId("user1", locked = false, expectedPageRequest) }
            coVerify(exactly = 1) { repository.lockAllById(setOf("a", "b", "c"), lock = true) }
            coVerify(exactly = 1) { repository.countAllByUserIdAndLockedTrue("user1") }
        }

        "unlock some" {
            // new limit is 10
            // user have 5 unlocked and 15 locked
            // should unlock 5

            val repository = mockk<AccountSubscriptionLimitRepository<String>>() {
                coEvery { countAllByUserId(any()) } returns 20
                coEvery { countAllByUserIdAndLockedTrue(any()) } returns 15
                coEvery { findIdsByUserId(any(), any(), any()) } returns setOf("a", "b", "c")
                coEvery { lockAllById(any(), any()) } returns Unit
            }

            AccountSubscriptionLimitUtils.maintainALimit(
                userId = "user1",
                newLimit = 10,
                baseSort = Sort.by("created"),
                repository = repository
            )

            val expectedPageRequest = PageRequest.of(0, 5, Sort.by("created").descending())
            coVerify(exactly = 1) { repository.countAllByUserId("user1") }
            coVerify(exactly = 1) { repository.countAllByUserIdAndLockedTrue("user1") }
            coVerify(exactly = 1) { repository.findIdsByUserId("user1", true, expectedPageRequest) }
            coVerify(exactly = 1) { repository.lockAllById(setOf("a", "b", "c"), lock = false) }
        }

        "dont do anything - less then limit" {
            // new limit is 10
            // user have 5 unlocked and 0 locked
            // shouldn't do anything

            val repository = mockk<AccountSubscriptionLimitRepository<String>>() {
                coEvery { countAllByUserId(any()) } returns 5
                coEvery { countAllByUserIdAndLockedTrue(any()) } returns 0
            }

            AccountSubscriptionLimitUtils.maintainALimit(
                userId = "user1",
                newLimit = 10,
                baseSort = Sort.by("created"),
                repository = repository
            )

            coVerify(exactly = 1) { repository.countAllByUserId("user1") }
            coVerify(exactly = 1) { repository.countAllByUserIdAndLockedTrue("user1") }
            coVerify(exactly = 0) { repository.lockAllById(any(), any()) }
            coVerify(exactly = 0) { repository.findIdsByUserId(any(), any(), any()) }
        }

        "dont do anything - more then limit, but locked" {
            // new limit is 10
            // user have 10 unlocked and 5 locked
            // shouldn't do anything

            val repository = mockk<AccountSubscriptionLimitRepository<String>>() {
                coEvery { countAllByUserId(any()) } returns 15
                coEvery { countAllByUserIdAndLockedTrue(any()) } returns 5
            }

            AccountSubscriptionLimitUtils.maintainALimit(
                userId = "user1",
                newLimit = 10,
                baseSort = Sort.by("created"),
                repository = repository
            )

            coVerify(exactly = 1) { repository.countAllByUserId("user1") }
            coVerify(exactly = 1) { repository.countAllByUserIdAndLockedTrue("user1") }
            coVerify(exactly = 0) { repository.lockAllById(any(), any()) }
            coVerify(exactly = 0) { repository.findIdsByUserId(any(), any(), any()) }
        }
    }

    "ReactiveMongoTemplate.lockAllById" {
        val expectedQuery = Query.query(Criteria.where("_id").inValues(setOf("a", "b", "c")))
        val expectedUpdate = BasicUpdate.update("lockFieldName", true)
        val updateResult = UpdateResult.acknowledged(1, 1, null)

        val mongoTemplate = mockk<ReactiveMongoTemplate>() {
            every { updateMulti(any(), any(), any<Class<*>>()) } returns updateResult.toMono()
        }

        mongoTemplate.lockAllById<HeartBeatSharing>(
            lockFieldName = "lockFieldName",
            ids = setOf("a", "b", "c"),
            lock = true
        )

        verify(exactly = 1) { mongoTemplate.updateMulti(expectedQuery, expectedUpdate, HeartBeatSharing::class.java) }
    }

    "ReactiveMongoTemplate.findIdsByUserId" {
        val expectedIds = setOf(ObjectId(), ObjectId(), ObjectId()).map { it.toHexString() }.toSet()
        val expectedQuery = Query.query(Criteria.where("userIdField").isEqualTo("user1")
            .and("lockedField").isEqualTo(true))
            .with(PageRequest.of(0, 20))
        expectedQuery.fields().include("_id")

        val mongoTemplate = mockk<ReactiveMongoTemplate>() {
            every { find(any(), any<Class<*>>(), any()) } returns Flux.fromIterable(expectedIds)
                .map { Document(mapOf("_id" to ObjectId(it))) }
        }

        val result = mongoTemplate.findIdsByUserId(
            collectionName = "testCollection",
            userIdFieldName = "userIdField",
            lockedFieldName = "lockedField",
            userId = "user1",
            pageable = PageRequest.of(0, 20),
            locked = true
        )
        result shouldBe expectedIds

        verify(exactly = 1) { mongoTemplate.find(expectedQuery, Document::class.java, "testCollection") }
    }
})