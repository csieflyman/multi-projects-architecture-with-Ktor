/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.report.data

import com.fasterxml.jackson.annotation.JsonIgnore
import kotlinx.serialization.Transient
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.typeOf

object ReportDataUtils {

    // TODO support nested object
    private val ignorePropertyTypes: List<KType> = listOf(typeOf<Iterable<*>>())

    fun getColumnIds(objClass: KClass<*>): List<String> {
        var columnIds = objClass.memberProperties.filter { property ->
            property.annotations.none { it.annotationClass == Transient::class || it.annotationClass == JsonIgnore::class } &&
                    !ignorePropertyTypes.contains(property.returnType)
        }.map { it.name }

        val propertyIndexMap = objClass.primaryConstructor?.parameters
            ?.mapIndexed { index, kParameter -> kParameter.name!! to index }?.toMap()
        if (propertyIndexMap != null)
            columnIds = columnIds.sortedBy { propertyIndexMap[it] ?: Int.MAX_VALUE }

        return columnIds
    }

    fun toMap(obj: Any, columnIds: List<String>): Map<String, Any?> =
        obj.javaClass.kotlin.memberProperties.filter { columnIds.contains(it.name) }.associate { it.name to it.get(obj) }
}