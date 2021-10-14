/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.database.util

import fanpoll.infra.base.entity.EntityDTO
import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.extension.filterNotNull
import fanpoll.infra.base.extension.myEquals
import fanpoll.infra.base.extension.myHashCode
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.database.sql.propName
import mu.KotlinLogging
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ExpressionAlias
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import kotlin.reflect.*
import kotlin.reflect.full.*

class ResultRowDTOMapper<T : EntityDTO<*>>(
    private val dtoClass: KClass<T>,
    val table: Table,
    private val columnNameAliasMap: Map<Column<*>, String>? = null,
    private val convertValueMap: Map<Column<*>, (Any?) -> Any?>? = null,
    private val joins: List<DynamicDBJoinPart>? = null,
) {

    private val propertyToColumnMap: Map<KProperty1<T, *>, Column<*>> = buildPropertyToColumnMap()
    private val propertyToDTOMapperMap: Map<KMutableProperty1<T, *>, ResultRowDTOMapper<T>> =
        buildNestedDTOPropertyToMapperMap()

    val columns = propertyToColumnMap.values.toList()

    private val fieldToColumnMap = propertyToColumnMap.mapKeys { it.key.name }
    private val fieldToDTOMapperMap = propertyToDTOMapperMap.mapKeys { it.key.name }

    companion object {

        private val logger = KotlinLogging.logger {}

        val objectType = typeOf<EntityDTO<*>?>()
        val listType = typeOf<List<EntityDTO<*>>?>()

        fun <T : EntityDTO<*>> getMapper(dtoClass: KClass<T>): ResultRowDTOMapper<T> {
            val companionObject =
                dtoClass.companionObject ?: error("${dtoClass.qualifiedName} must have companion object")
            val companionObjectInstance = dtoClass.companionObjectInstance
            return companionObject.memberProperties.first { it.name == "mapper" }
                .getter.call(companionObjectInstance) as ResultRowDTOMapper<T>
        }
    }

    fun mapDTO(resultRow: ResultRow): T? {
        return newDTOWithPKValues(resultRow)?.also { mapDTO(it, resultRow) }
    }

    private fun newDTOWithPKValues(resultRow: ResultRow): T? {
        val dtoIdValues = resultRow.getPKValuesOfTable(table)
        return dtoIdValues?.let { dtoClass.primaryConstructor!!.call(*it.toTypedArray()) }
    }

    fun mapDTO(dto: T, resultRow: ResultRow) {
        val properties = dto.javaClass.kotlin.memberProperties.filterIsInstance<KMutableProperty1<T, *>>()

        properties.filter { propertyToColumnMap.containsKey(it) }.forEach { property ->
            val column = propertyToColumnMap[property]!!
            val value = if (resultRow.hasValue(column)) {
                val value = (resultRow[column] as? EntityID<*>)?.value ?: resultRow[column]
                convertValueMap?.get(column)?.let { it(value) } ?: value
            } else null

            logger.debug { "column property => ${property.name} = $value (${value?.javaClass?.kotlin?.qualifiedName})" }
            if (value != null) {
                property.setter.call(dto, value)
            }
        }

        properties.filter { !propertyToColumnMap.containsKey(it) && !propertyToDTOMapperMap.containsKey(it) }
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
                    property.setter.call(dto, value)
                }
            }

        properties.filter { propertyToDTOMapperMap.containsKey(it) }.forEach { property ->
            logger.debug { "===== nestedDTO ${property.name} begin =====" }
            val nestedDTOMapper = propertyToDTOMapperMap[property]!!
            val nestedDTO = nestedDTOMapper.newDTOWithPKValues(resultRow)
            logger.debug { "nestedDTO = ${nestedDTO?.getId()} (${nestedDTO?.javaClass?.kotlin?.qualifiedName})" }

            val value: Any? = if (nestedDTO != null) {
                nestedDTOMapper.mapDTO(nestedDTO, resultRow)
                if (property.returnType.isSubtypeOf(listType)) {
                    mutableListOf(nestedDTO)
                } else nestedDTO
            } else null

            logger.debug { "===== nestedDTO ${property.name} end =====" }
            if (value != null) {
                property.setter.call(dto, value)
            }
        }
    }

    private fun buildPropertyToColumnMap(): Map<KProperty1<T, *>, Column<*>> {
        val map = mutableMapOf<KProperty1<T, *>, Column<*>>()

        val idPropertiesMap = dtoClass.primaryConstructor!!.parameters.associate { parameter ->
            val property = dtoClass.memberProperties.single { it.name == parameter.name }
            val column = table.columns.single { column ->
                val columnNameAlias = columnNameAliasMap?.get(column) ?: column.propName
                columnNameAlias == parameter.name
            }
            property to column
        }

        val propertiesMap =
            dtoClass.memberProperties.filterIsInstance<KMutableProperty1<T, *>>().associateWith { property ->
                table.columns.find { column ->
                    val columnNameAlias = columnNameAliasMap?.get(column) ?: column.propName
                    columnNameAlias == property.name
                }
            }.filterNotNull()

        map.putAll(idPropertiesMap)
        map.putAll(propertiesMap)
        return map
    }

    private fun buildNestedDTOPropertyToMapperMap(): Map<KMutableProperty1<T, *>, ResultRowDTOMapper<T>> {
        return dtoClass.memberProperties.filterIsInstance<KMutableProperty1<T, *>>().associateWith { property ->
            val nestedDTOClass = when {
                property.returnType.isSubtypeOf(listType) -> property.returnType.arguments[0].type!!.classifier as KClass<T>
                property.returnType.isSubtypeOf(objectType) -> property.returnType.classifier as KClass<T>
                else -> null
            }
            nestedDTOClass?.let { getMapper(it) }
        }.filterNotNull()
    }

    private fun getTable(field: String, mapper: ResultRowDTOMapper<T> = this): Table {
        val head = field.substringBefore(".", field)
        return if (mapper.fieldToDTOMapperMap.containsKey(head)) {
            if (head == field)
                mapper.fieldToDTOMapperMap[head]!!.table
            else
                getTable(field.substringAfter(".", field), mapper.fieldToDTOMapperMap[head]!!)
        } else {
            mapper.fieldToColumnMap[head]?.table ?: throw RequestException(
                InfraResponseCode.BAD_REQUEST_QUERYSTRING,
                "field $field is undefined"
            )
        }
    }

    // fields must in order
    fun getRelationTables(fields: List<String>): List<Table> = fields.map { getTable(it) }.filter { it != table }.distinct()

    fun getJoin(table: Table): DynamicDBJoinPart = joins!!.first { it.otherTable == table }

    fun getColumn(field: String): Column<*> = getColumns(field).single()

    fun getColumns(field: String): List<Column<*>> {
        return if (!field.contains(".")) {
            fieldToColumnMap[field]?.let { listOf(it) }
                ?: fieldToDTOMapperMap[field]?.propertyToColumnMap?.values?.toList()
                ?: throw RequestException(InfraResponseCode.BAD_REQUEST_QUERYSTRING, "field $field is undefined")
        } else {
            val head = field.substringBefore(".")
            val tail = field.substringAfter(".")
            fieldToDTOMapperMap[head]?.getColumns(tail)
                ?: throw RequestException(InfraResponseCode.BAD_REQUEST_QUERYSTRING, "field $head is undefined")
        }
    }

    override fun equals(other: Any?): Boolean = myEquals(other, { dtoClass })

    override fun hashCode(): Int = myHashCode({ dtoClass })

    override fun toString(): String = "mapper(dtoClass = ${dtoClass.qualifiedName}, table = ${table.tableName}, " +
            "columnNameAliasMap = $columnNameAliasMap, convertValueMapKeys = ${convertValueMap?.mapKeys { it.key.name }?.keys})"
}

private val logger = KotlinLogging.logger {}

private fun ResultRow.getPKValueOfTable(table: Table): Any? = this[table.primaryKey!!.columns.single()]

private fun ResultRow.getPKValuesOfTable(table: Table): List<Any>? = table.primaryKey!!.columns
    .filter { this.hasValue(it) }
    .mapNotNull { column -> (this[column] as? EntityID<*>)?.value ?: this[column] }
    .toList().takeIf { it.isNotEmpty() }

fun <T : EntityDTO<*>> ResultRow.toDTO(dtoClass: KClass<T>): T {
    logger.debug { "========== map ResultRow to ${dtoClass.qualifiedName} begin ==========" }
    val mapper = ResultRowDTOMapper.getMapper(dtoClass)
    val value = mapper.mapDTO(this)!!
    logger.debug { "========== map ResultRow to ${dtoClass.qualifiedName} end ==========" }
    return value
}

fun <T : EntityDTO<*>> List<ResultRow>.toSingleDTO(dtoClass: KClass<T>): T? {
    return takeIf { it.isNotEmpty() }?.toDTO(dtoClass)?.get(0)
}

fun <T : EntityDTO<*>> List<ResultRow>.toDTO(dtoClass: KClass<T>): List<T> {
    logger.debug { "=============== map List<ResultRow> to ${dtoClass.qualifiedName} begin ===============" }
    val mapper = ResultRowDTOMapper.getMapper(dtoClass)
    val dtoList = mutableListOf<T>()
    val dtoMap = mutableMapOf<List<*>, T>()

    this.forEach { resultRow ->
        val dtoIdValues = resultRow.getPKValuesOfTable(mapper.table)!!
        var dto = dtoMap[dtoIdValues]
        if (dto == null) {
            dto = dtoClass.primaryConstructor!!.call(*dtoIdValues.toTypedArray())
            mapper.mapDTO(dto, resultRow)
            dtoMap[dtoIdValues] = dto
            dtoList.add(dto)
        } else {
            // use single to avoid cross join
            val nestedDTOListProperty =
                dtoClass.memberProperties.single { it.returnType.isSubtypeOf(ResultRowDTOMapper.listType) } as KProperty<MutableList<T>>
            val nestedDTOClass = nestedDTOListProperty.returnType.arguments[0].type!!.classifier as KClass<T>
            val nestedDTO = resultRow.toDTO(nestedDTOClass)
            nestedDTOListProperty.getter.call(dto).add(nestedDTO)
        }
    }
    logger.debug { "=============== map List<ResultRow> to ${dtoClass.qualifiedName} end ===============" }
    return dtoList
}