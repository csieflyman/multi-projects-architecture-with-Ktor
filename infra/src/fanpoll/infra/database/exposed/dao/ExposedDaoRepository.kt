/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.database.exposed.dao

import fanpoll.infra.base.entity.Entity
import fanpoll.infra.base.entity.GenericRepository
import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.database.exposed.sql.dbExecute
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.sql.Database
import kotlin.reflect.KClass

class ExposedDaoRepository<E : Entity<ID>, ID : Comparable<ID>>(
    private val entityClass: KClass<E>,
    private val exposedEntityClass: EntityClass<ID, *>,
    private val db: Database
) : GenericRepository<E, ID> {

    override suspend fun createAndGetId(entity: E): ID = dbExecute(db) {
        exposedEntityClass.new(entity).id.value
    }

    override suspend fun create(entity: E): Unit = dbExecute(db) {
        exposedEntityClass.new(entity)
    }

    override suspend fun update(entity: E) = dbExecute(db) {
        exposedEntityClass.update(entity)
    }

    override suspend fun delete(id: ID) = dbExecute(db) {
        exposedEntityClass.findById(id)?.delete()
    } ?: throw RequestException(InfraResponseCode.ENTITY_NOT_EXIST, "entity $id does not exist")

    override suspend fun getById(id: ID): E {
        return findById(id) ?: throw RequestException(InfraResponseCode.ENTITY_NOT_EXIST, "entity $id does not exist")
    }

    override suspend fun findById(id: ID): E? = dbExecute(db) {
        exposedEntityClass.findById(id)?.toObject(entityClass)
    }

    override suspend fun findAll(): List<E> = dbExecute(db) {
        exposedEntityClass.all().map { it.toObject(entityClass) }
    }
}