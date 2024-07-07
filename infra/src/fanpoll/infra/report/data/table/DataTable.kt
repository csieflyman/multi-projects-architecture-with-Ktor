/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.report.data.table

import com.fasterxml.jackson.annotation.JsonIgnore
import fanpoll.infra.base.json.kotlinx.json
import fanpoll.infra.report.data.DatasetItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.typeOf

@Serializable
class DataTable(override val id: String, val name: String) : DatasetItem() {

    val columns: MutableList<DataColumn> = mutableListOf()

    val data: MutableList<MutableList<DataCell>> = mutableListOf()

    override fun toJson(): JsonElement = json.encodeToJsonElement(this)

    companion object {
        operator fun invoke(
            id: String,
            name: String,
            objectClass: KClass<*>,
            objects: List<Any>,
            columnIds: List<String>? = null
        ): DataTable {
            val table = DataTable(id, name)
            val columns = getColumns(objectClass, columnIds)
            val data = getData(objects, columns.map { it.id }.toList())
            table.columns.addAll(columns)
            table.data.addAll(data)
            return table
        }

        private fun getColumns(objectClass: KClass<*>, columnIds: List<String>?): List<DataColumn> =
            getObjectPropertiesMap(objectClass, columnIds).map { DataColumn(it.key, it.key, it.value.simpleName!!) }

        private fun getData(objects: List<Any>, columnIds: List<String>): MutableList<MutableList<DataCell>> = objects.map { obj ->
            obj.javaClass.kotlin.memberProperties.filter { columnIds.contains(it.name) }
                .sortedBy { columnIds.indexOf(it.name) }.map { DataCell(it.get(obj)) }.toMutableList()
        }.toMutableList()

        private fun getObjectPropertiesMap(objClass: KClass<*>, columnIds: List<String>?): Map<String, KClass<*>> {
            val ignorePropertyTypes: List<KType> = listOf(typeOf<Iterable<*>>()) // nested object is not support
            val propertyIndexMap = objClass.primaryConstructor?.parameters
                ?.mapIndexed { index, kParameter -> kParameter.name!! to index }?.toMap() ?: mapOf()
            return objClass.memberProperties.filter { property ->
                columnIds?.contains(property.name) ?: true &&
                        property.annotations.none { it.annotationClass == Transient::class || it.annotationClass == JsonIgnore::class } &&
                        !ignorePropertyTypes.contains(property.returnType)
            }.associate { it.name to it.returnType.classifier as KClass<*> }
                .entries.sortedWith(compareBy { propertyIndexMap[it.key] ?: Int.MAX_VALUE })
                .associate { it.key to it.value }
        }
    }
}

