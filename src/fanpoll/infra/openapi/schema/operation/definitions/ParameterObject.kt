/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.schema.operation.definitions

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import fanpoll.infra.openapi.schema.operation.support.Definition
import fanpoll.infra.openapi.schema.operation.support.Example
import fanpoll.infra.openapi.schema.operation.support.Header
import fanpoll.infra.openapi.schema.operation.support.Parameter

enum class ParameterInputType {
    header,
    path,
    query,
    cookie,
    body, // Not supported in OpenAPI v3.
}

open class ParameterObject(
    open val `in`: ParameterInputType,
    val required: Boolean,
    val schema: PropertyDef,
    @JsonIgnore val description: String? = null,
    val deprecated: Boolean? = null,
    val allowEmptyValue: Boolean? = null,
    @JsonIgnore val example: Any? = null,
    @JsonIgnore val examples: Map<String, Example>? = null
) : Definition("${schema.getDefinition().name}${if (required) "" else "-optional"}"), Parameter {

    override fun componentsFieldName(): String = "parameters"

    @JsonProperty("name")
    open val parameterName: String = schema.getDefinition().name

    override fun defPair(): Pair<String, ParameterObject> = name to this

    override fun valuePair(): Pair<String, Parameter> = if (hasRef()) refPair() else defPair()

    @JsonGetter("description")
    fun getDescriptionValue(): String? {
        return description ?: schema.description?.let { "[schema => $it]" }
    }

    @JsonGetter("example")
    fun getExampleValue(): Any? {
        return example ?: schema.example
    }

    @JsonGetter("examples")
    fun getExamplesValue(): Map<String, Example>? {
        return examples ?: schema.examples
    }
}

class HeaderObject(
    required: Boolean,
    schema: PropertyDef,
    description: String? = null,
    deprecated: Boolean? = null,
    allowEmptyValue: Boolean? = null,
    example: Any? = null,
    examples: Map<String, Example>? = null
) : ParameterObject(
    ParameterInputType.header, required, schema, description,
    deprecated, allowEmptyValue, example, examples
), Header {

    override fun componentsFieldName(): String = "headers"

    @JsonIgnore
    override val parameterName: String = super.parameterName

    @JsonIgnore
    override val `in`: ParameterInputType = super.`in`

    override fun defPair(): Pair<String, HeaderObject> = name to this

    override fun valuePair(): Pair<String, Header> = if (hasRef()) refPair() else defPair()
}