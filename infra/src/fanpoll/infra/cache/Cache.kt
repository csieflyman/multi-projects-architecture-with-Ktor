/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.cache

import fanpoll.infra.base.entity.Entity
import fanpoll.infra.base.entity.EntityDTO
import fanpoll.infra.base.exception.EntityException
import fanpoll.infra.base.response.ResponseCode
import fanpoll.infra.database.dao.toDTO
import org.jetbrains.exposed.dao.EntityClass

interface Cache<K : Any, V : Any> {

    suspend fun get(key: K): V?

    suspend fun set(key: K, value: V, expirationMs: Long? = null)

    suspend fun remove(key: K)
}

suspend fun <EC : EntityClass<ID, E>, ID : Comparable<ID>, E> EC.findByEntityId(id: ID, cache: Cache<ID, E>): E?
        where E : org.jetbrains.exposed.dao.Entity<ID>, E : Entity<ID> {
    return cache.get(id) ?: findById(id)?.also { cache.set(id, it) }
}

suspend fun <EC : EntityClass<ID, E>, ID : Comparable<ID>, E> EC.getById(id: ID, cache: Cache<ID, E>): E
        where E : org.jetbrains.exposed.dao.Entity<ID>, E : Entity<ID> {
    return findByEntityId(id, cache) ?: throw EntityException(
        ResponseCode.ENTITY_NOT_FOUND, "can't find entity by entityId from cache and db", entityId = id
    )
}

suspend inline fun <EC : EntityClass<ID, E>, ID : Comparable<ID>, E, reified D : EntityDTO<ID>> EC.findDTOByEntityId(
    id: ID,
    cache: Cache<ID, D>
): D? where E : org.jetbrains.exposed.dao.Entity<ID>, E : Entity<ID> {
    return cache.get(id) ?: findById(id)?.let {
        val dto = it.toDTO(D::class)
        cache.set(id, dto)
        dto
    }
}

suspend inline fun <EC : EntityClass<ID, E>, ID : Comparable<ID>, E, reified D : EntityDTO<ID>> EC.getDTOByEntityId(
    id: ID,
    cache: Cache<ID, D>
): D where E : org.jetbrains.exposed.dao.Entity<ID>, E : Entity<ID> {
    return findDTOByEntityId(id, cache) ?: throw EntityException(
        ResponseCode.ENTITY_NOT_FOUND, "can't find dto by entityId from cache and db", entityId = id
    )
}