/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.database.exposed.jasync

import fanpoll.infra.base.entity.Entity
import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.response.InfraResponseCode
import kotlinx.coroutines.Deferred
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import kotlin.reflect.KClass

class JasyncRepository<E : Entity<ID>, ID : Comparable<ID>>(
    private val entityClass: KClass<E>,
    private val table: IdTable<ID>,
    private val db: Database
) {
    suspend fun createAsync(entity: E) {
        jasyncTransaction(db) {
            table.jasyncInsert(entity)
        }
    }

    suspend fun updateAsync(entity: E) {
        jasyncTransaction(db) {
            table.jasyncUpdate(entity)
        }
    }

    suspend fun deleteAsync(id: ID) {
        jasyncTransaction(db) {
            table.jasyncDelete { (table.primaryKey!!.columns[0] as Column<ID>).eq(id) }
        }
    }

    suspend fun getById(id: ID): Deferred<E> {
        return jasyncQuery(db) {
            table.selectAll().where { (table.primaryKey!!.columns[0] as Column<ID>).eq(id) }
                .jasyncSingleDTOOrNull(entityClass)
                ?: throw RequestException(InfraResponseCode.ENTITY_NOT_EXIST, "entity $id does not exist")
        }
    }

    suspend fun findAll(): Deferred<List<E>> {
        return jasyncQuery(db) {
            table.selectAll().jasyncToList(entityClass)
        }
    }
}