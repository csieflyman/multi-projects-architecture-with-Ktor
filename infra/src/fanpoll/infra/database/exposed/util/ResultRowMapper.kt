/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.database.exposed.util

import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.extension.filterValuesNotNull
import fanpoll.infra.base.extension.myEquals
import fanpoll.infra.base.extension.myHashCode
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.database.exposed.sql.propName
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.typeOf

class ResultRowMapper<T : Any>(
    val objClass: KClass<T>,
    val table: Table,
    private val columnNameAliasMap: Map<Column<*>, String>? = null,
    private val convertValueMap: Map<Column<*>, (Any?) -> Any?>? = null,
    private val joinTypeMap: Map<Table, JoinType>? = null
) {
    private val logger = KotlinLogging.logger {}

    private val collectionType = typeOf<Collection<*>?>()
    private val listType = typeOf<List<*>?>()
    private val setType = typeOf<Set<*>?>()
    private val propertyToColumnMap: Map<KProperty1<T, *>, Column<*>> = buildPropertyToColumnMap()
    private val propertyToObjectMapperMap: Map<KMutableProperty1<T, *>, ResultRowMapper<T>> = buildNestedObjectPropertyToMapperMap()

    fun toObject(resultRow: ResultRow): T? {
        logger.debug { "toObjectClass = " + objClass.qualifiedName }
        val obj = newObjectInstance(resultRow)
        return obj?.apply { mapRow(obj, resultRow) }
    }

    // object's primaryConstructor parameters correspond to columns
    private fun newObjectInstance(resultRow: ResultRow): T? {
        val parameterMap = objClass.primaryConstructor!!.parameters.associateWith { objectKParameter ->
            val column = propertyToColumnMap.filterKeys { prop -> prop.name == objectKParameter.name }.values.first()
            if (resultRow.hasValue(column)) {
                val value = (resultRow[column] as? EntityID<*>)?.value ?: resultRow[column]
                convertValueMap?.get(column)?.let { it(value) } ?: value
            } else null
        }
        logger.debug { "parameterMap = $parameterMap" }
        return if (parameterMap.values.any { it == null }) null
        else objClass.primaryConstructor!!.callBy(parameterMap)
    }

    private fun mapRow(obj: T, resultRow: ResultRow) {
        mapObjectProperties(obj, resultRow)
        mapObjectAliasProperties(obj, resultRow)
        mapNestedObjects(obj, resultRow)
    }

    private fun mapObjectProperties(obj: T, resultRow: ResultRow) {
        objClass.memberProperties.filterIsInstance<KMutableProperty1<T, *>>()
            .filter { property -> propertyToColumnMap.containsKey(property) }.forEach { property ->
                val column = propertyToColumnMap[property]!!
                val value = if (resultRow.hasValue(column)) {
                    val value = (resultRow[column] as? EntityID<*>)?.value ?: resultRow[column]
                    convertValueMap?.get(column)?.let { it(value) } ?: value
                } else null

                logger.debug { "column property => ${property.name} = $value (${value?.javaClass?.kotlin?.qualifiedName})" }
                if (value != null) {
                    property.setter.call(obj, value)
                }
            }
    }

    private fun mapObjectAliasProperties(obj: T, resultRow: ResultRow) {
        objClass.memberProperties.filterIsInstance<KMutableProperty1<T, *>>()
            .filter { !propertyToColumnMap.containsKey(it) && !propertyToObjectMapperMap.containsKey(it) }
            .forEach { property ->
                val exprAlias = resultRow.fieldIndex.keys.find { expr ->
                    (expr as? ExpressionAlias<*>)?.let { it.alias == property.name } ?: false
                }
                val value = exprAlias?.let { resultRow[it] }

                if (exprAlias != null)
                    logger.debug { "alias property => ${property.name} = $value (${value?.javaClass?.kotlin?.qualifiedName})" }
//                    else
//                        logger.debug { "unmapping property => ${property.name}" }
                if (value != null) {
                    property.setter.call(obj, value)
                }
            }
    }

    private fun mapNestedObjects(obj: T, resultRow: ResultRow) {
        objClass.memberProperties.filterIsInstance<KMutableProperty1<T, *>>()
            .filter { propertyToObjectMapperMap.containsKey(it) }.forEach { property ->
                logger.debug { "===== nestedDTO ${property.name} begin =====" }
                val nestedObjectMapper = propertyToObjectMapperMap[property]
                val nestedObject = nestedObjectMapper?.newObjectInstance(resultRow)
                logger.debug { "nestedDTO = (${nestedObject?.javaClass?.kotlin?.qualifiedName})" }

                val value: Any? = if (nestedObject != null) {
                    nestedObjectMapper.mapRow(nestedObject, resultRow)
                    if (property.returnType.isSubtypeOf(listType)) {
                        mutableListOf(nestedObject)
                    } else if (property.returnType.isSubtypeOf(setType)) {
                        mutableSetOf(nestedObject)
                    } else {
                        throw InternalServerException(InfraResponseCode.DEV_ERROR, "nested object only support List or Set type")
                    }
                } else null

                logger.debug { "===== nestedDTO ${property.name} end =====" }
                if (value != null) {
                    property.setter.call(obj, value)
                }
            }
    }

    private fun buildIdPropertiesToColumnMap(): Map<KProperty1<T, *>, Column<*>> {
        return objClass.primaryConstructor!!.parameters.associate { parameter ->
            val property = objClass.memberProperties.single { it.name == parameter.name }
            val column = table.columns.single { column ->
                val columnNameAlias = columnNameAliasMap?.get(column) ?: column.propName
                columnNameAlias == parameter.name
            }
            property to column
        }
    }

    private fun buildPropertyToColumnMap(): Map<KProperty1<T, *>, Column<*>> {
        val map = mutableMapOf<KProperty1<T, *>, Column<*>>()
        val idPropertiesMap = buildIdPropertiesToColumnMap()
        val propertiesMap =
            objClass.memberProperties.filterIsInstance<KMutableProperty1<T, *>>().associateWith { property ->
                table.columns.find { column ->
                    val columnNameAlias = columnNameAliasMap?.get(column) ?: column.propName
                    columnNameAlias == property.name
                }
            }.filterValuesNotNull()

        map.putAll(idPropertiesMap)
        map.putAll(propertiesMap)
        return map
    }

    private fun buildNestedObjectPropertyToMapperMap(): Map<KMutableProperty1<T, *>, ResultRowMapper<T>> {
        return objClass.memberProperties.filterIsInstance<KMutableProperty1<T, *>>().associateWith { property ->
            val nestedObjectClass = when {
                property.returnType.javaClass.isPrimitive || property.returnType.javaClass.isEnum -> null
                property.returnType.isSubtypeOf(collectionType) -> {
                    if (property.returnType.arguments.isEmpty()) null
                    else {
                        val nestedKType = property.returnType.arguments[0].type!!
                        if (nestedKType.javaClass.isPrimitive || nestedKType.javaClass.isEnum) null
                        else nestedKType.classifier as KClass<T>
                    }
                }

                else -> property.returnType.classifier as KClass<T>
            }
            nestedObjectClass?.let { ResultRowMappers.getMapper(it) }
        }.filterValuesNotNull()
    }

    private val fieldToColumnMap = propertyToColumnMap.mapKeys { it.key.name }
    private val fieldToObjectMapperMap = propertyToObjectMapperMap.mapKeys { it.key.name }

    fun getColumn(field: String): Column<*> = getColumns(field).single()

    fun getColumns(field: String): List<Column<*>> {
        return if (!field.contains(".")) {
            fieldToColumnMap[field]?.let { listOf(it) }
                ?: fieldToObjectMapperMap[field]?.propertyToColumnMap?.values?.toList()
                ?: throw RequestException(InfraResponseCode.BAD_REQUEST_QUERYSTRING, "field $field is undefined")
        } else {
            val head = field.substringBefore(".")
            val tail = field.substringAfter(".")
            fieldToObjectMapperMap[head]?.getColumns(tail)
                ?: throw RequestException(InfraResponseCode.BAD_REQUEST_QUERYSTRING, "field $head is undefined")
        }
    }

    // fields must be in hierarchical for sql join table
    fun getTables(fields: List<String>): List<Table> = fields.map { getTable(it) }.filter { it != table }.distinct()

    private fun getTable(field: String, mapper: ResultRowMapper<T> = this): Table {
        val head = field.substringBefore(".", field)
        return if (mapper.fieldToObjectMapperMap.containsKey(head)) {
            if (head == field)
                mapper.fieldToObjectMapperMap[head]!!.table
            else
                getTable(field.substringAfter(".", field), mapper.fieldToObjectMapperMap[head]!!)
        } else {
            mapper.fieldToColumnMap[head]?.table ?: throw RequestException(
                InfraResponseCode.BAD_REQUEST_QUERYSTRING,
                "field $field is undefined"
            )
        }
    }

    fun getJoinType(table: Table): JoinType = joinTypeMap?.get(table) ?: JoinType.INNER

    override fun equals(other: Any?): Boolean = myEquals(other, { objClass })

    override fun hashCode(): Int = myHashCode({ objClass })

    override fun toString(): String = "mapper(objClass = ${objClass.qualifiedName}, table = ${table.tableName}, " +
            "columnNameAliasMap = $columnNameAliasMap, convertValueMapKeys = ${convertValueMap?.mapKeys { it.key.name }?.keys})"
}