/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.schema.component.definitions

import fanpoll.infra.openapi.schema.component.support.SecurityScheme
import fanpoll.infra.openapi.schema.operation.definitions.*
import fanpoll.infra.openapi.schema.operation.support.Definition
import fanpoll.infra.openapi.schema.operation.support.Schema
import kotlin.reflect.KClass

class ComponentsObject {
    val securitySchemes: MutableMap<String, SecuritySchemeObject> = mutableMapOf()
    val headers: MutableMap<String, HeaderObject> = mutableMapOf()
    val parameters: MutableMap<String, ParameterObject> = mutableMapOf()
    val requestBodies: MutableMap<String, RequestBodyObject> = mutableMapOf()
    val responses: MutableMap<String, ResponseObject> = mutableMapOf()
    val schemas: MutableMap<String, SchemaObject> = mutableMapOf()
    val examples: MutableMap<String, ExampleObject> = mutableMapOf()
    //val links: MutableMap<String, Any> = mutableMapOf()
    //val callbacks: MutableMap<String, Any> = mutableMapOf()

    private val schemaRefMap: MutableMap<KClass<*>, Schema> = mutableMapOf()

    fun getSchemaRef(modelClass: KClass<*>): Schema? = schemaRefMap[modelClass]

    fun add(securityScheme: SecurityScheme) {
        securitySchemes += securityScheme.name to securityScheme.value
    }

    fun add(referenceObject: ReferenceObject) = add(referenceObject.getDefinition())

    fun add(definition: Definition): ReferenceObject {
        when (definition) {
            is HeaderObject -> headers += definition.name to definition
            is ParameterObject -> parameters += definition.name to definition
            is RequestBodyObject -> requestBodies += definition.name to definition
            is ResponseObject -> responses += definition.name to definition
            is SchemaObject -> schemas += definition.name to definition
            is ExampleObject -> examples += definition.name to definition
        }
        val ref = definition.createRef()
        if (definition is SchemaObject && definition.kClass != null)
            schemaRefMap[definition.kClass] = ref
        return ref
    }
}
