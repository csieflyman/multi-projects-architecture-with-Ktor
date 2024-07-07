/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.database.exposed.sql

import fanpoll.infra.base.entity.Entity
import fanpoll.infra.base.entity.GenericRepository
import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.response.InfraResponseCode
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import kotlin.reflect.KClass

class ExposedDSLRepository<E : Entity<ID>, ID : Comparable<ID>>(
    private val entityClass: KClass<E>,
    private val table: IdTable<ID>,
    private val db: Database
) : GenericRepository<E, ID> {
    override suspend fun createAndGetId(entity: E): ID = dbExecute(db) {
        table.insertAndGetId(entity)
    }

    override suspend fun create(entity: E): Unit = dbExecute(db) {
        table.insert(entity)
    }

    override suspend fun update(entity: E): Unit = dbExecute(db) {
        table.update(entity)
    }

    override suspend fun delete(id: ID): Unit = dbExecute(db) {
        table.deleteWhere { (table.primaryKey!!.columns[0] as Column<ID>).eq(id) }
    }

    override suspend fun getById(id: ID): E {
        return findById(id) ?: throw RequestException(InfraResponseCode.ENTITY_NOT_EXIST, "entity $id does not exist")
    }

    override suspend fun findById(id: ID): E? = dbExecute(db) {
        table.selectAll().where { (table.primaryKey!!.columns[0] as Column<ID>).eq(id) }
            .singleOrNull(entityClass)
    }

    override suspend fun findAll(): List<E> = dbExecute(db) {
        table.selectAll().toList(entityClass)
    }
}