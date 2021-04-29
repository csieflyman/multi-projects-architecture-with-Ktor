/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.schema.operation.definitions

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import fanpoll.infra.openapi.schema.operation.support.Definition
import fanpoll.infra.openapi.schema.operation.support.Schema
import fanpoll.infra.utils.Jackson
import kotlin.reflect.KClass

enum class SchemaDataType {
    string, number, integer, boolean, array, `object`
}

abstract class SchemaObject(
    name: String,
    val type: SchemaDataType,
    val description: String? = null,
    refName: String? = null,
    @JsonIgnore var parent: Schema?,
    @get:JsonIgnore val kClass: KClass<*>? = null
) : Definition(name, refName), Schema {

    override fun componentsFieldName(): String = "schemas"

    @JsonIgnore
    override fun getId(): String = path

    @get:JsonIgnore
    var kPropertyNullable: Boolean? = null

    @JsonIgnore
    val path: String = ((parent?.getDefinition() as? SchemaObject)?.path ?: "") + "/$name"

    fun debugString(): String =
        if (parent == null) name
        else "$path (${kClass?.qualifiedName}${if (kPropertyNullable == true) "?" else ""})"

    override fun defPair(): Pair<String, SchemaObject> = name to this

    override fun valuePair(): Pair<String, Schema> = if (hasRef()) refPair() else defPair()
}

open class PropertyDef(
    name: String,
    dataType: SchemaDataType,
    description: String? = null,
    refName: String? = null,
    parent: ModelDef? = null,
    kClass: KClass<*>? = null,
    val format: String? = null,
    val pattern: String? = null,
    val minimum: Int? = null,
    val maximum: Int? = null,
    val exclusiveMinimum: Int? = null,
    val exclusiveMaximum: Int? = null,
    val minLength: Int? = null,
    val maxLength: Int? = null,
    var enum: List<Any>? = null,
    val default: Any? = null,
    val example: Any? = null
) : SchemaObject(name, dataType, description, refName, parent, kClass)

class ModelDef(
    name: String,
    val required: List<String>? = null,
    val properties: Map<String, Schema>,
    description: String? = null,
    refName: String? = null,
    parent: ModelDef? = null,
    kClass: KClass<*>? = null,
    val example: Any? = null
) : SchemaObject(name, SchemaDataType.`object`, description, refName, parent, kClass)

class ArrayPropertyDef(
    name: String,
    val items: Schema,
    val uniqueItems: Boolean? = null,
    description: String? = null,
    refName: String? = null,
    parent: ModelDef? = null,
    kClass: KClass<*>? = null
) : SchemaObject(name, SchemaDataType.array, description, refName, parent, kClass)

class ArrayModelDef(
    name: String,
    val items: Schema,
    description: String? = null,
    refName: String? = null,
    parent: ModelDef? = null,
    kClass: KClass<*>? = null
) : SchemaObject(name, SchemaDataType.array, description, refName, parent, kClass)

class DictionaryPropertyDef(
    name: String,
    description: String? = null,
    refName: String? = null,
    parent: ModelDef? = null,
    kClass: KClass<*>? = null,
) : PropertyDef(name, SchemaDataType.`object`, description, refName, parent, kClass) {

    val additionalProperties = Jackson.newObject()
}

abstract class ComplexSchema(@get:JsonIgnore override val name: String) : Schema {

    @JsonIgnore
    override fun getDefinition(): Definition = error("complex schema is not a definition")

    @JsonIgnore
    override fun getReference(): ReferenceObject = error("complex schema can't be referenced")
}

class OneOfSchema(name: String, val oneOf: List<ReferenceObject>) : ComplexSchema(name)

class AllOfSchema(name: String, private val parent: ReferenceObject, private val child: ModelDef) : ComplexSchema(name) {

    @JsonValue
    fun toJson(): JsonNode {
        return Jackson.newObject().set("allOf", Jackson.newArray().add(Jackson.toJson(parent)).add(Jackson.toJson(child)))
    }
}