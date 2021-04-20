/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.database

import fanpoll.infra.EntityException
import fanpoll.infra.ResponseCode
import fanpoll.infra.controller.EntityDTO
import fanpoll.infra.controller.EntityForm
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import java.sql.SQLIntegrityConstraintViolationException
import kotlin.reflect.full.memberProperties

fun <T> T.insert(form: EntityForm<*, *, *>, body: (T.(InsertStatement<Number>) -> Unit)? = null): Any? where T : Table, T : DTOTable<*> {
    val dtoProps = form::class.memberProperties
    val result = try {
        insert { insertStatement ->
            (columns as List<Column<Any>>).forEach { column ->
                dtoProps.find { dtoProp -> autoIncColumn != column && dtoProp.name == column.propName }
                    ?.let { dtoProp -> dtoProp.getter.call(form)?.let { insertStatement[column] = it } }
            }
            body?.invoke(this, insertStatement)
        }
    } catch (e: ExposedSQLException) {
        // COMPATIBILITY => MySQL: "Duplicate key", PostgreSQL: ""
        if (e.cause is SQLIntegrityConstraintViolationException) {
            if (e.message!!.contains("Duplicate", true))
                throw EntityException(ResponseCode.ENTITY_DUPLICATED, "entity should be unique", e, entityId = form.tryGetId())
            else
                throw EntityException(ResponseCode.ENTITY_PROP_INVALID, null, e, entityId = form.tryGetId())
        } else throw e
    }
    return (this as? IdTable<*>)?.let { result[it.id].value }
}

fun <T> T.update(form: EntityForm<*, *, *>, body: (T.(UpdateStatement) -> Unit)? = null): Int
        where T : Table, T : DTOTable<*> {
    val pkColumns = primaryKey!!.columns.toList() as List<Column<Any>>
    val columnMap = columns.associateBy { it.propName } as Map<String, Column<Any>>
    val updateColumnMap = columnMap.filterKeys { it != "createTime" && it != "updateTime" }
        .filterValues { !pkColumns.contains(it) }

    val table = this
    val where: (SqlExpressionBuilder.() -> Op<Boolean>) = { buildFindByDTOOp(table, form) }

    val myBody: T.(UpdateStatement) -> Unit = { updateStatement ->
        form::class.memberProperties.forEach { dtoProp ->
            updateColumnMap[dtoProp.name]?.let { column ->
                dtoProp.getter.call(form)?.let { dtoPropValue -> updateStatement[column] = dtoPropValue }
            }
        }
        body?.invoke(this, updateStatement)
    }

    val size = update(where, null, myBody)
    if (size == 0)
        throw EntityException(ResponseCode.ENTITY_NOT_FOUND, entityId = form.getId())
    return size
}

fun <T> T.insertOrUpdate(
    form: EntityForm<*, *, *>,
    insertBody: (T.(InsertStatement<Number>) -> Unit)? = null,
    updateBody: (T.(UpdateStatement) -> Unit)? = null
): Boolean
        where T : Table, T : DTOTable<*> {
    return when (update(form, updateBody)) {
        0 -> {
            insert(form, insertBody)
            true
        }
        1 -> false
        else -> throw EntityException(ResponseCode.ENTITY_DUPLICATED, "entity should be unique", entityId = form.getId())
    }
}

inline fun <T, ID : Comparable<ID>, reified D : EntityDTO<ID>> T.findByEntityId(id: ID): D?
        where T : Table, T : DTOTable<ID> {
    val table = this
    return select { buildFindByIdOp(table, id, true) }.singleOrNull()?.toDTO(D::class)
}

inline fun <T, ID : Comparable<ID>, reified D : EntityDTO<ID>> T.getByEntityId(id: ID): D
        where T : Table, T : DTOTable<ID> {
    return findByEntityId(id) ?: throw EntityException(ResponseCode.ENTITY_NOT_FOUND, "can't find entity by entityId", entityId = id)
}

inline fun <T, ID : Comparable<ID>, reified D : EntityDTO<ID>> T.findByDTOId(id: Any): D?
        where T : Table, T : DTOTable<ID> {
    val table = this
    return select { buildFindByIdOp(table, id, false) }.singleOrNull()?.toDTO(D::class)
}

inline fun <T, ID : Comparable<ID>, reified D : EntityDTO<ID>> T.getByDTOId(id: Any): D
        where T : Table, T : DTOTable<ID> {
    return findByDTOId(id) ?: throw EntityException(ResponseCode.ENTITY_NOT_FOUND, "can't find entity by dtoId", entityId = id)
}

fun buildFindByIdOp(table: DTOTable<*>, id: Any, entityId: Boolean): Op<Boolean> {
    return if (entityId) {
        require(table.surrogateKey != null)
        (table.surrogateKey!! as Column<Any>).eq(id)
    } else {
        require(table.naturalKeys != null)
        if (table.naturalKeys!!.size == 1) {
            (table.naturalKeys!![0] as Column<Any>).eq(id)
        } else {
            table.naturalKeys!!.zip(id as List<*>).fold(Op.TRUE as Op<Boolean>) { result, (column, value) ->
                result and ((column as Column<Any>).eq(value!!))
            }
        }
    }
}

private fun buildFindByDTOOp(table: DTOTable<*>, dto: EntityForm<*, *, *>): Op<Boolean> {
    return if (table.surrogateKey != null && dto.getEntityId() != null) {
        (table.surrogateKey!! as Column<Any>).eq(dto.getEntityId()!!)
    } else {
        require(table.naturalKeys != null && dto.getDtoId() != null)
        if (table.naturalKeys!!.size == 1) {
            (table.naturalKeys!![0] as Column<Any>).eq(dto.getDtoId()!!)
        } else {
            table.naturalKeys!!.zip(dto.getDtoId() as List<*>).fold(Op.TRUE as Op<Boolean>) { result, (column, value) ->
                result and ((column as Column<Any>).eq(value!!))
            }
        }
    }
}