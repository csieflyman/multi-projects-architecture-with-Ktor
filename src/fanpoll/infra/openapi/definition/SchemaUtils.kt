/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.definition

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import fanpoll.infra.utils.DateTimeUtils
import kotlinx.serialization.SerialName
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonArray
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
import kotlin.reflect.typeOf

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class OpenApiSchemaIgnore

object SchemaUtils {

    private val logger = KotlinLogging.logger {}

    fun toModelDef(components: Components, name: String, modelKType: KType): Schema {
        return toSchema(components, name, modelKType) ?: run {
            val modelClass = modelKType.classifier as KClass<*>
            val modelProperties = getModelProperties(modelClass)
            val requiredProperties = mutableListOf<String>()
            val modelPropertySchemas = modelProperties.map { kProperty ->
                val propertyName = getModelPropertyName(kProperty)
                val propertyKType = kProperty.returnType
                val propertyClass = propertyKType.classifier as KClass<*>
                val propertySchema = toSchema(components, propertyName, propertyKType)
                    ?: toPropertyDef(propertyName, propertyClass) ?: toModelDef(components, propertyName, propertyKType)
                if (propertySchema is SchemaDef)
                    propertySchema.kPropertyNullable = kProperty.returnType.isMarkedNullable
                if (!kProperty.returnType.isMarkedNullable)
                    requiredProperties += propertyName
                propertySchema
            }.sortedWith(propertySchemaSortComparator)

            val modelSchema = ModelDef(
                name, requiredProperties.takeIf { it.isNotEmpty() },
                modelPropertySchemas.map { it.name to it }.toMap(), kClass = modelClass
            ).also {
                it.properties.values.forEach { property -> (property.definition as? SchemaDef)?.parent = it }
            }

            logger.debug { "====================" }
            logger.debug { modelSchema.debugString() }
            modelPropertySchemas.forEach { logger.debug { (it.definition as SchemaDef).debugString() } }

            modelSchema
        }
    }

    private fun toSchema(components: Components, name: String, modelKType: KType): Schema? {
        val modelClass = modelKType.classifier as KClass<*>
        return components.getReuseSchema(modelClass)?.definition?.createRef(name) ?: getArrayModelDefKType(modelKType)
            ?.let { toArrayDef(components, name, it) }
    }

    private fun toArrayDef(components: Components, name: String, arrayDefKType: KType): SchemaDef {
        val arrayDefClass = arrayDefKType.classifier as KClass<*>
        return toPropertyDef(name, arrayDefClass)?.let { ArrayPropertyDef(name, it, kClass = arrayDefClass) }
            ?: toArrayModelDef(components, name, arrayDefKType)
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
                    name, SchemaDataType.string, format = "date-time", pattern = DateTimeUtils.UTC_DATE_TIME_PATTERN,
                    kClass = Instant::class
                )
                Date::class -> PropertyDef(
                    name, SchemaDataType.string, format = "date-time", pattern = DateTimeUtils.UTC_DATE_TIME_PATTERN,
                    kClass = Date::class
                )
                ZonedDateTime::class -> PropertyDef(
                    name, SchemaDataType.string, format = "date-time", pattern = DateTimeUtils.UTC_DATE_TIME_PATTERN,
                    kClass = ZonedDateTime::class
                )
                LocalDateTime::class -> PropertyDef(
                    name, SchemaDataType.string, format = "date-time", pattern = DateTimeUtils.LOCAL_DATE_TIME_PATTERN,
                    kClass = LocalDateTime::class
                )
                LocalDate::class -> PropertyDef(
                    name, SchemaDataType.string, format = "date", pattern = DateTimeUtils.LOCAL_DATE_PATTERN,
                    kClass = LocalDate::class
                )
                LocalTime::class -> PropertyDef(
                    name, SchemaDataType.string, pattern = DateTimeUtils.LOCAL_TIME_PATTERN,
                    kClass = LocalTime::class
                )
                YearMonth::class -> PropertyDef(
                    name, SchemaDataType.string, pattern = DateTimeUtils.YEAR_MONTH_PATTERN,
                    kClass = YearMonth::class
                )
                Year::class -> PropertyDef(
                    name, SchemaDataType.string, pattern = DateTimeUtils.YEAR_PATTERN,
                    kClass = Year::class
                )

                Map::class -> DictionaryPropertyDef(name, description = "Map<String, Any>", kClass = Map::class)
                JsonObject::class -> DictionaryPropertyDef(name, description = "JsonObject", kClass = JsonObject::class)
                JsonNode::class -> DictionaryPropertyDef(name, description = "JsonObject", kClass = JsonNode::class)
                ObjectNode::class -> DictionaryPropertyDef(name, description = "JsonObject", kClass = ObjectNode::class)

                else -> null
            }
        }
    }

    private val CollectionKType = typeOf<Collection<*>?>()
    private val KotlinJsonArrayKType = typeOf<JsonArray?>()
    private val JacksonJsonArrayKType = typeOf<ArrayNode?>()

    private fun getArrayModelDefKType(modelKType: KType): KType? = when {
        modelKType.isSubtypeOf(KotlinJsonArrayKType) || modelKType.isSubtypeOf(JacksonJsonArrayKType) -> modelKType
        modelKType.isSubtypeOf(CollectionKType) -> modelKType.arguments[0].type!!
        else -> null
    }

    private fun toArrayModelDef(components: Components, name: String, arrayModelDefKType: KType): SchemaDef {
        val modelDef = when {
            arrayModelDefKType.isSubtypeOf(KotlinJsonArrayKType) ||
                    arrayModelDefKType.isSubtypeOf(JacksonJsonArrayKType) -> DictionaryPropertyDef(
                name, description = "JsonObject of JsonArray"
            )
            else -> toModelDef(components, name, arrayModelDefKType)
        }
        return ArrayModelDef(name, modelDef, kClass = arrayModelDefKType.classifier as KClass<*>)
    }

    private val ignoreModelTypes: List<KType> = listOf()

    private fun getModelProperties(modelClass: KClass<*>): Collection<KProperty1<*, *>> {
        return modelClass.memberProperties.filter { property ->
            property.annotations.none {
                it.annotationClass == Transient::class ||
                        it.annotationClass == JsonIgnore::class ||
                        it.annotationClass == OpenApiSchemaIgnore::class
            } && !ignoreModelTypes.contains(property.returnType)
        }
    }

    fun getModelName(modelKType: KType): String = if (modelKType.isSubtypeOf(CollectionKType))
        (modelKType.arguments[0].type!!.classifier as KClass<*>).simpleName + "Array"
    else (modelKType.classifier as KClass<*>).simpleName!!

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

    // CUSTOMIZATION
    private val propertyNameOrder = listOf("id", "account", "password", "name", "enabled")

    private val propertySchemaSortComparator =
        Comparator.comparingInt<Schema> {
            when {
                propertyNameOrder.contains(it.name) -> propertyNameOrder.indexOf(it.name)
                (it.definition as SchemaDef).kPropertyNullable == false -> propertyNameOrder.size + 1
                else -> propertyNameOrder.size + 2
            }
        }.thenComparingInt {
            when (it) {
                is ReferenceObject -> 0
                is ModelDef -> 1
                is ArrayModelDef -> 2
                is PropertyDef -> 3
                is ArrayPropertyDef -> 4
                is DictionaryPropertyDef -> 5
                else -> 6
            }
        }

}