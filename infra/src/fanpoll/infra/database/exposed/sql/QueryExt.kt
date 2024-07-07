/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.database.exposed.sql

import fanpoll.infra.database.exposed.util.ResultRowMappers
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.typeOf

private val logger = KotlinLogging.logger {}

fun <T : Any> Query.toList(objectClass: KClass<T>): List<T> {
    return toList().toObject(objectClass)
}

fun <T : Any> Query.singleOrNull(objectClass: KClass<T>): T? {
    return singleOrNull()?.toObject(objectClass)
}

fun <T : Any> ResultRow.toObject(objectClass: KClass<T>): T? {
    logger.debug { "========== map ResultRow to ${objectClass.qualifiedName} begin ==========" }
    val mapper = ResultRowMappers.getMapper(objectClass)
    val value = mapper?.toObject(this)
    logger.debug { "========== map ResultRow to ${objectClass.qualifiedName} end ==========" }
    return value
}

fun <T : Any> List<ResultRow>.toSingleDTOOrNull(objectClass: KClass<T>): T? {
    return getOrNull(0)?.toObject(objectClass)
}

fun <T : Any> List<ResultRow>.toObject(objectClass: KClass<T>): List<T> {
    logger.debug { "=============== map List<ResultRow> to ${objectClass.qualifiedName} begin ===============" }
    val mapper = ResultRowMappers.getMapper(objectClass)!!
    val objList = mutableListOf<T>()
    val objMap = mutableMapOf<List<*>, T>()
    val collectionType = typeOf<Collection<*>?>()
    val nestedObjectProperties = objectClass.memberProperties
        .filter {
            it.returnType.isSubtypeOf(collectionType) && it.returnType.arguments.isNotEmpty() &&
                    ResultRowMappers.getMapper(it.returnType.arguments[0].type!!.classifier as KClass<T>) != null
        } as List<KProperty<MutableCollection<T>>>

    this.forEach { resultRow ->
        val objIdValues = resultRow.getPKValuesOfTable(mapper.table)!!
        var obj = objMap[objIdValues]
        if (obj == null) {
            obj = mapper.toObject(resultRow)!!
            objMap[objIdValues] = obj
            objList.add(obj)
        } else {
            // avoid cross join
            nestedObjectProperties.forEach { property ->
                val nestedObjectClass = property.returnType.arguments[0].type!!.classifier as KClass<T>
                val nestedDTO = resultRow.toObject(nestedObjectClass)
                if (nestedDTO != null)
                    property.getter.call(obj).add(nestedDTO)
            }
        }
    }
    logger.debug { "=============== map List<ResultRow> to ${objectClass.qualifiedName} end ===============" }
    return objList
}

fun ResultRow.getPKValuesOfTable(table: Table): List<Any>? = table.primaryKey!!.columns
    .filter { this.hasValue(it) }
    .mapNotNull { column -> (this[column] as? EntityID<*>)?.value ?: this[column] }
    .toList().takeIf { it.isNotEmpty() }