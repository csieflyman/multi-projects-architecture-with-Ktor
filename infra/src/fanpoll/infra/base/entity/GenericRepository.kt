/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.base.entity

interface GenericRepository<E : Entity<ID>, ID : Comparable<ID>> {

    suspend fun createAndGetId(entity: E): ID

    suspend fun create(entity: E)

    suspend fun update(entity: E)

    suspend fun delete(id: ID)

    suspend fun getById(id: ID): E

    suspend fun findById(id: ID): E?

    suspend fun findAll(): List<E>
}