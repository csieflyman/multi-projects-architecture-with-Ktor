/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.schema.operation.support.converters

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import fanpoll.infra.base.util.DateTimeUtils
import fanpoll.infra.openapi.schema.component.definitions.ComponentsObject
import fanpoll.infra.openapi.schema.operation.definitions.*
import fanpoll.infra.openapi.schema.operation.support.OpenApiIgnore
import fanpoll.infra.openapi.schema.operation.support.OpenApiModel
import fanpoll.infra.openapi.schema.operation.support.Schema
import fanpoll.infra.openapi.schema.operation.support.utils.DataModelUtils.CollectionKType
import fanpoll.infra.openapi.schema.operation.support.utils.DataModelUtils.JacksonJsonArrayKType
import fanpoll.infra.openapi.schema.operation.support.utils.DataModelUtils.KotlinxJsonArrayKType
import fanpoll.infra.openapi.schema.operation.support.utils.DataModelUtils.getSchemaName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject
import mu.KotlinLogging
import java.math.BigDecimal
import java.time.*
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties

object SchemaObjectConverter {

    private val logger = KotlinLogging.logger {}

    private val propertyConverters: MutableMap<KClass<*>, (String) -> PropertyDef> = mutableMapOf()

    fun registerPropertyConverter(propertyClass: KClass<*>, converter: (String) -> PropertyDef) {
        val currentConverter = propertyConverters.putIfAbsent(propertyClass, converter)
        if (currentConverter != null) error("propertyClass ${propertyClass.qualifiedName} converter had been registered")
    }

    fun toSchema(components: ComponentsObject, modelKType: KType, modelName: String? = null): Schema {
        val name = modelName ?: getSchemaName(modelKType)
        val modelClass = modelKType.classifier as KClass<*>
        return components.getSchemaRef(modelClass)
            ?: toPropertyDef(name, modelClass)
            ?: getArrayDefKType(modelKType)?.let { toArrayDef(components, name, it) }
            ?: toModelDef(components, name, modelKType)
    }

    private fun getArrayDefKType(modelKType: KType): KType? = when {
        modelKType.isSubtypeOf(KotlinxJsonArrayKType) || modelKType.isSubtypeOf(JacksonJsonArrayKType) -> modelKType
        modelKType.isSubtypeOf(CollectionKType) -> modelKType.arguments[0].type!!
        else -> null
    }

    private fun toArrayDef(components: ComponentsObject, name: String, arrayDefKType: KType): SchemaObject {
        val arrayDefClass = arrayDefKType.classifier as KClass<*>
        return toPropertyDef(name, arrayDefClass)?.let { ArrayPropertyDef(name, it, kClass = arrayDefClass) }
            ?: toArrayModelDef(components, name, arrayDefKType)
    }

    private fun toArrayModelDef(components: ComponentsObject, name: String, arrayModelDefKType: KType): SchemaObject {
        val modelDef = when {
            arrayModelDefKType.isSubtypeOf(KotlinxJsonArrayKType) ||
                    arrayModelDefKType.isSubtypeOf(JacksonJsonArrayKType) -> DictionaryPropertyDef(
                name, description = "JsonObject of JsonArray"
            )
            else -> toModelDef(components, name, arrayModelDefKType)
        }
        return ArrayModelDef(name, modelDef, kClass = arrayModelDefKType.classifier as KClass<*>)
    }

    private fun toModelDef(components: ComponentsObject, name: String, modelKType: KType): Schema {
        val modelClass = modelKType.classifier as KClass<*>
        val modelProperties = getModelProperties(modelClass)
        val requiredProperties = mutableListOf<String>()
        var modelPropertySchemas = modelProperties.map { kProperty ->
            val propertyName = getModelPropertyName(kProperty)
            val propertyKType = kProperty.returnType
            val propertySchema = toSchema(components, propertyKType, propertyName)
            if (propertySchema is SchemaObject)
                propertySchema.kPropertyNullable = kProperty.returnType.isMarkedNullable
            if (!kProperty.returnType.isMarkedNullable)
                requiredProperties += propertyName
            propertySchema
        }

        modelPropertySchemas = modelClass.annotations.find { it.annotationClass == OpenApiModel::class }?.let {
            modelPropertySchemas.sortedWith(createPropertyComparator((it as OpenApiModel).propertyNameOrder))
        } ?: modelPropertySchemas

        val modelSchema = ModelDef(
            name, requiredProperties.takeIf { it.isNotEmpty() },
            modelPropertySchemas.associateBy { it.name }, kClass = modelClass
        ).also {
            it.properties.values.forEach { property -> (property.getDefinition() as? SchemaObject)?.parent = it }
        }

        logger.debug { "====================" }
        logger.debug { modelSchema.debugString() }
        modelPropertySchemas.forEach { logger.debug { (it.getDefinition() as SchemaObject).debugString() } }

        return modelSchema
    }

    fun toPropertyDef(name: String, propertyClass: KClass<*>): PropertyDef? {
        return if (propertyClass.java.isEnum) {
            PropertyDef(
                name, SchemaDataType.string,
                enum = propertyClass.java.enumConstants.map { it.toString() }.toList(),
                kClass = propertyClass
            )
        } else {
            when (propertyClass) {
                String::class -> PropertyDef(name, SchemaDataType.string, kClass = String::class)
                UUID::class -> PropertyDef(name, SchemaDataType.string, format = "uuid", kClass = UUID::class)

                Boolean::class -> PropertyDef(name, SchemaDataType.boolean, kClass = String::class)

                Int::class -> PropertyDef(name, SchemaDataType.integer, format = "int32", kClass = Int::class)
                Long::class -> PropertyDef(name, SchemaDataType.integer, format = "int64", kClass = Long::class)

                Double::class -> PropertyDef(
                    name, SchemaDataType.number, format = "double", description = "Double", kClass = Double::class
                )
                BigDecimal::class -> PropertyDef(
                    name, SchemaDataType.number, format = "double", description = "BigDecimal", kClass = BigDecimal::class
                )

                ZoneId::class -> PropertyDef(
                    name, SchemaDataType.string, default = DateTimeUtils.TAIWAN_ZONE_ID,
                    kClass = ZoneId::class
                )
                Instant::class -> PropertyDef(
                    name, SchemaDataType.string, format = "date-time", description = DateTimeUtils.UTC_DATE_TIME_PATTERN,
                    kClass = Instant::class
                )
                Date::class -> PropertyDef(
                    name, SchemaDataType.string, format = "date-time", description = DateTimeUtils.UTC_DATE_TIME_PATTERN,
                    kClass = Date::class
                )
                ZonedDateTime::class -> PropertyDef(
                    name, SchemaDataType.string, format = "date-time", description = DateTimeUtils.UTC_DATE_TIME_PATTERN,
                    kClass = ZonedDateTime::class
                )
                LocalDateTime::class -> PropertyDef(
                    name, SchemaDataType.string, format = "date-time", description = DateTimeUtils.LOCAL_DATE_TIME_PATTERN,
                    kClass = LocalDateTime::class
                )
                LocalDate::class -> PropertyDef(
                    name, SchemaDataType.string, format = "date", description = DateTimeUtils.LOCAL_DATE_PATTERN,
                    kClass = LocalDate::class
                )
                LocalTime::class -> PropertyDef(
                    name, SchemaDataType.string, description = DateTimeUtils.LOCAL_TIME_PATTERN,
                    kClass = LocalTime::class
                )
                YearMonth::class -> PropertyDef(
                    name, SchemaDataType.string, description = DateTimeUtils.YEAR_MONTH_PATTERN,
                    kClass = YearMonth::class
                )
                Year::class -> PropertyDef(
                    name, SchemaDataType.string, description = DateTimeUtils.YEAR_PATTERN,
                    kClass = Year::class
                )

                Map::class -> DictionaryPropertyDef(name, description = "Map<String, Any>", kClass = Map::class)
                JsonObject::class -> DictionaryPropertyDef(name, description = "JsonObject", kClass = JsonObject::class)
                JsonNode::class -> DictionaryPropertyDef(name, description = "JsonObject", kClass = JsonNode::class)
                ObjectNode::class -> DictionaryPropertyDef(name, description = "JsonObject", kClass = ObjectNode::class)

                else -> propertyConverters[propertyClass]?.invoke(name)
            }
        }
    }

    private fun getModelProperties(modelClass: KClass<*>): Collection<KProperty1<*, *>> {
        return if (modelClass.annotations.any { it.annotationClass == OpenApiIgnore::class })
            emptyList()
        else
            modelClass.memberProperties.filter { property ->
                property.annotations.none {
                    it.annotationClass == Transient::class ||
                            it.annotationClass == JsonIgnore::class ||
                            it.annotationClass == OpenApiIgnore::class
                }
            }
    }

    private fun getModelPropertyName(kProperty: KProperty1<*, *>): String = run loop@{
        kProperty.annotations.forEach {
            val name = when (it.annotationClass) {
                SerialName::class -> (it as SerialName).value
                JsonProperty::class -> (it as JsonProperty).value
                JsonGetter::class -> (it as JsonGetter).value
                else -> null
            }
            if (name != null)
                return@loop name
        }
        kProperty.name
    }

    private fun createPropertyComparator(propertyNameOrder: Array<String>): Comparator<Schema> = Comparator.comparingInt<Schema> {
        when {
            propertyNameOrder.contains(it.name) -> propertyNameOrder.indexOf(it.name)
            (it.getDefinition() as SchemaObject).kPropertyNullable == false -> propertyNameOrder.size + 1
            else -> propertyNameOrder.size + 2
        }
    }.thenComparingInt {
        when (it) {
            is ReferenceObject -> 0
            is ModelDef -> 1
            is ArrayModelDef -> 2
            is DictionaryPropertyDef -> 5
            is PropertyDef -> 3
            is ArrayPropertyDef -> 4
            else -> 6
        }
    }
}