/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.database.sql

import fanpoll.infra.base.entity.EntityDTO
import fanpoll.infra.base.entity.EntityForm
import fanpoll.infra.base.exception.EntityException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.database.util.toDTO
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import java.sql.SQLIntegrityConstraintViolationException
import java.util.*
import kotlin.reflect.full.memberProperties

interface Table<EID : Comparable<EID>> {

    val naturalKeys: List<Column<out Any>>?

    val surrogateKey: Column<EntityID<EID>>?
}

abstract class IdTable<EID : Comparable<EID>>(name: String = "") : org.jetbrains.exposed.dao.id.IdTable<EID>(name), Table<EID>

abstract class LongIdTable(name: String = "", columnName: String = "id") : IdTable<Long>(name) {
    override val id: Column<EntityID<Long>> = long(columnName).autoIncrement().entityId()
    override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(id) }
}

abstract class UUIDTable(name: String = "", columnName: String = "id", autoGenerate: Boolean = true) : IdTable<UUID>(name) {
    override val id: Column<EntityID<UUID>> = uuid(columnName).let { if (autoGenerate) it.autoGenerate() else it }.entityId()
    override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(id) }
}

fun <T> T.insert(
    form: EntityForm<*, *, *>,
    body: (T.(InsertStatement<Number>) -> Unit)? = null
): Any? where T : org.jetbrains.exposed.sql.Table, T : Table<*> {
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
                throw EntityException(InfraResponseCode.ENTITY_ALREADY_EXISTS, "entity should be unique", e, entityId = form.tryGetId())
            else
                throw EntityException(InfraResponseCode.ENTITY_PROP_VALUE_INVALID, null, e, entityId = form.tryGetId())
        } else throw e
    }
    return (this as? IdTable<*>)?.let { result[it.id].value }
}

fun <T> T.update(
    form: EntityForm<*, *, *>,
    body: (T.(UpdateStatement) -> Unit)? = null
): Int where T : org.jetbrains.exposed.sql.Table, T : Table<*> {
    val pkColumns = primaryKey!!.columns.toList() as List<Column<Any>>
    val columnMap = columns.associateBy { it.propName } as Map<String, Column<Any>>
    val updateColumnMap = columnMap.filterKeys { it != "createdAt" && it != "updatedAt" }
        .filterValues { !pkColumns.contains(it) }

    val table = this
    val where: (SqlExpressionBuilder.() -> Op<Boolean>) = { entityEq(table, form) }

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
        throw EntityException(InfraResponseCode.ENTITY_NOT_FOUND, entityId = form.getId())
    return size
}

fun <T> T.insertOrUpdate(
    form: EntityForm<*, *, *>,
    insertBody: (T.(InsertStatement<Number>) -> Unit)? = null,
    updateBody: (T.(UpdateStatement) -> Unit)? = null
): Boolean where T : org.jetbrains.exposed.sql.Table, T : Table<*> = when (update(form, updateBody)) {
    0 -> {
        insert(form, insertBody)
        true
    }
    1 -> false
    else -> throw EntityException(InfraResponseCode.ENTITY_ALREADY_EXISTS, "entity should be unique", entityId = form.getId())
}

inline fun <T, ID : Comparable<ID>, reified D : EntityDTO<ID>> T.findByEntityId(id: ID): D? where T : org.jetbrains.exposed.sql.Table, T : Table<ID> {
    val table = this
    return select { entityIdEq(table, id) }.singleOrNull()?.toDTO(D::class)
}

inline fun <T, ID : Comparable<ID>, reified D : EntityDTO<ID>> T.getByEntityId(id: ID): D where T : org.jetbrains.exposed.sql.Table, T : Table<ID> {
    return findByEntityId(id) ?: throw EntityException(InfraResponseCode.ENTITY_NOT_FOUND, "can't find entity by entityId", entityId = id)
}

inline fun <T, ID : Comparable<ID>, reified D : EntityDTO<ID>> T.findByDTOId(id: Any): D? where T : org.jetbrains.exposed.sql.Table, T : Table<ID> {
    val table = this
    return select { dtoIdEq(table, id) }.singleOrNull()?.toDTO(D::class)
}

inline fun <T, ID : Comparable<ID>, reified D : EntityDTO<ID>> T.getByDTOId(id: Any): D where T : org.jetbrains.exposed.sql.Table, T : Table<ID> {
    return findByDTOId(id) ?: throw EntityException(InfraResponseCode.ENTITY_NOT_FOUND, "can't find entity by dtoId", entityId = id)
}

fun SqlExpressionBuilder.entityIdEq(table: Table<*>, id: Any): Op<Boolean> {
    require(table.surrogateKey != null)
    return (table.surrogateKey!! as Column<Any>).eq(id)
}

fun SqlExpressionBuilder.dtoIdEq(table: Table<*>, id: Any): Op<Boolean> {
    require(table.naturalKeys != null)
    return if (table.naturalKeys!!.size == 1) {
        (table.naturalKeys!![0] as Column<Any>).eq(id)
    } else {
        table.naturalKeys!!.zip(id as List<*>).fold(Op.TRUE as Op<Boolean>) { result, (column, value) ->
            result and ((column as Column<Any>).eq(value!!))
        }
    }
}

fun SqlExpressionBuilder.entityEq(table: Table<*>, form: EntityForm<*, *, *>): Op<Boolean> {
    return if (table.surrogateKey != null && form.getEntityId() != null) {
        (table.surrogateKey!! as Column<Any>).eq(form.getEntityId()!!)
    } else {
        require(table.naturalKeys != null && form.getDtoId() != null)
        if (table.naturalKeys!!.size == 1) {
            (table.naturalKeys!![0] as Column<Any>).eq(form.getDtoId()!!)
        } else {
            table.naturalKeys!!.zip(form.getDtoId() as List<*>).fold(Op.TRUE as Op<Boolean>) { result, (column, value) ->
                result and ((column as Column<Any>).eq(value!!))
            }
        }
    }
}