/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.schema.operation.definitions

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import fanpoll.infra.openapi.schema.operation.support.*
import fanpoll.infra.utils.Jackson

class ReferenceObject(
    override val name: String,
    @JvmField private val definition: Definition
) : Element, Header, Parameter, RequestBody, Response, Schema, Example {

    val `$ref` = "#/components/${definition.componentsFieldName()}/${definition.name}"

    override fun getDefinition(): Definition = definition

    override fun getReference(): ReferenceObject = this

    override fun createRef(): ReferenceObject = this

    override fun valuePair(): Pair<String, Element> = name to this

    @JsonValue
    fun toJson(): JsonNode = Jackson.newObject().put("\$ref", `$ref`)
}