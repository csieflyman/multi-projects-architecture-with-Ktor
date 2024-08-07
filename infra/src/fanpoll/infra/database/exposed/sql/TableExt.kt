/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.database.exposed.sql

import fanpoll.infra.base.entity.Identifiable
import fanpoll.infra.base.exception.EntityException
import fanpoll.infra.base.response.InfraResponseCode
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.JavaInstantColumnType
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.statements.UpsertStatement
import java.sql.SQLIntegrityConstraintViolationException
import kotlin.reflect.full.memberProperties

fun <ID : Comparable<ID>> IdTable<ID>.insertAndGetId(
    obj: Any,
    body: (IdTable<*>.(InsertStatement<*>) -> Unit)? = null
): ID {
    val result = try {
        insertAndGetId { insertStatement ->
            fillInsertColumns(obj, insertStatement)
            body?.invoke(this, insertStatement)
        }.value
    } catch (e: ExposedSQLException) {
        handleInsertException(e, obj)
    }
    return result
}

fun <ID : Comparable<ID>> IdTable<ID>.insert(
    obj: Any,
    body: (Table.(InsertStatement<*>) -> Unit)? = null
): ID {
    val result = try {
        insert { insertStatement ->
            fillInsertColumns(obj, insertStatement)
            body?.invoke(this, insertStatement)
        }
    } catch (e: ExposedSQLException) {
        handleInsertException(e, obj)
    }
    return result[id].value
}

private fun <ID : Comparable<ID>> IdTable<ID>.fillInsertColumns(obj: Any, insertStatement: InsertStatement<*>) {
    (columns as List<Column<Any>>).forEach { column ->
        obj::class.memberProperties.find { objProp -> autoIncColumn != column && objProp.name == column.propName }
            ?.let { objProp ->
                objProp.getter.call(obj)?.let {
                    if (column.columnType is EntityIDColumnType<*>)
                        insertStatement[column] = EntityID(it as ID, this)
                    else
                        insertStatement[column] = it
                }
            }
    }
}

private fun handleInsertException(e: ExposedSQLException, obj: Any): Nothing {
    // COMPATIBILITY => MySQL: "Duplicate key", PostgreSQL: ""
    if (e.cause is SQLIntegrityConstraintViolationException) {
        if (e.message!!.contains("Duplicate", true))
            throw EntityException(InfraResponseCode.ENTITY_ALREADY_EXISTS, "entity should be unique: $obj", e)
        else
            throw EntityException(InfraResponseCode.ENTITY_PROP_VALUE_INVALID, "entity $obj", e)
    } else throw e
}

fun <ID : Comparable<ID>> Table.update(
    obj: Identifiable<ID>,
    body: (Table.(UpdateStatement) -> Unit)? = null
): Int {
    val idColumn = primaryKey!!.columns.first() as Column<ID>
    val ignoreUpdateColumns = ignoreUpdateColumns()
    val updateColumnMap = columns.filterNot { ignoreUpdateColumns.contains(it) }
        .associateBy { it.propName } as Map<String, Column<Any>>
    val myBody: Table.(UpdateStatement) -> Unit = { updateStatement ->
        obj::class.memberProperties.forEach { objProp ->
            updateColumnMap[objProp.name]?.let { column ->
                objProp.getter.call(obj)?.let { objPropValue -> updateStatement[column] = objPropValue }
            }
        }
        body?.invoke(this, updateStatement)
    }
    val size = update({ idColumn.eq(obj.getId()) }, null, myBody)
    if (size == 0)
        throw EntityException(InfraResponseCode.ENTITY_NOT_FOUND)
    return size
}

fun <ID : Comparable<ID>> Table.upsert(
    obj: Identifiable<ID>,
    body: Table.(UpsertStatement<Long>) -> Unit
): UpsertStatement<Long> {
    val idColumn = primaryKey!!.columns.first() as Column<ID>
    val onUpdateExclude = ignoreUpdateColumns()
    val updateColumnMap = columns.filterNot { onUpdateExclude.contains(it) }
        .associateBy { it.propName } as Map<String, Column<Any>>
    val myBody: Table.(UpsertStatement<Long>) -> Unit = { statement ->
        statement[idColumn] = obj.getId()
        obj::class.memberProperties.forEach { objProp ->
            updateColumnMap[objProp.name]?.let { column ->
                objProp.getter.call(obj)?.let { objPropValue -> statement[column] = objPropValue }
            }
        }
        body.invoke(this, statement)
    }
    return this.upsert(idColumn, onUpdateExclude = onUpdateExclude, body = myBody)
}

fun Table.ignoreUpdateColumns(): List<Column<*>> = columns.filter {
    (it.name == "createdAt" && it.columnType is JavaInstantColumnType) ||
            (it.name == "updatedAt" && it.columnType is JavaInstantColumnType) ||
            primaryKey!!.columns.contains(it)
}