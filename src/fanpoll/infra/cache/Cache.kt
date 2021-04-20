/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.cache

import fanpoll.infra.EntityException
import fanpoll.infra.ResponseCode
import fanpoll.infra.controller.EntityDTO
import fanpoll.infra.database.toDTO
import fanpoll.infra.model.Entity
import org.jetbrains.exposed.dao.EntityClass

interface Cache<K : Any, V : Any> {

    fun get(key: K): V?

    suspend fun getAsync(key: K): V?

    fun put(key: K, value: V, expirationMs: Long? = null)

    suspend fun putAsync(key: K, value: V, expirationMs: Long? = null)

    suspend fun invalidate(key: K)

    fun shutdown()
}

fun <EC : EntityClass<ID, E>, ID : Comparable<ID>, E> EC.findByEntityId(id: ID, cache: Cache<ID, E>): E?
        where E : org.jetbrains.exposed.dao.Entity<ID>, E : Entity<ID> {
    return cache.get(id) ?: findById(id)?.also { cache.put(id, it) }
}

fun <EC : EntityClass<ID, E>, ID : Comparable<ID>, E> EC.getById(id: ID, cache: Cache<ID, E>): E
        where E : org.jetbrains.exposed.dao.Entity<ID>, E : Entity<ID> {
    return findByEntityId(id, cache) ?: throw EntityException(
        ResponseCode.ENTITY_NOT_FOUND, "can't find entity by entityId from cache and db", entityId = id
    )
}

inline fun <EC : EntityClass<ID, E>, ID : Comparable<ID>, E, reified D : EntityDTO<ID>> EC.findDTOByEntityId(
    id: ID,
    cache: Cache<ID, D>
): D? where E : org.jetbrains.exposed.dao.Entity<ID>, E : Entity<ID> {
    return cache.get(id) ?: findById(id)?.let {
        val dto = it.toDTO(D::class)
        cache.put(id, dto)
        dto
    }
}

inline fun <EC : EntityClass<ID, E>, ID : Comparable<ID>, E, reified D : EntityDTO<ID>> EC.getDTOByEntityId(
    id: ID,
    cache: Cache<ID, D>
): D where E : org.jetbrains.exposed.dao.Entity<ID>, E : Entity<ID> {
    return findDTOByEntityId(id, cache) ?: throw EntityException(
        ResponseCode.ENTITY_NOT_FOUND, "can't find dto by entityId from cache and db", entityId = id
    )
}