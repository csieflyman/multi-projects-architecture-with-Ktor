/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.definition

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlin.reflect.KClass

class Components(securitySchemes: List<SecurityScheme>) {
    val securitySchemes: Map<String, SecuritySchemeObject> = securitySchemes.map { it.name to it.value }.toMap()
    val headers: MutableMap<String, HeaderDef> = mutableMapOf()
    val parameters: MutableMap<String, ParameterDef> = mutableMapOf()
    val requestBodies: MutableMap<String, RequestBodiesDef> = mutableMapOf()
    val responses: MutableMap<String, ResponseDef> = mutableMapOf()
    val schemas: MutableMap<String, SchemaDef> = mutableMapOf()
    val examples: MutableMap<String, ExampleDef> = mutableMapOf()
    //val links: MutableMap<String, Any> = mutableMapOf()
    //val callbacks: MutableMap<String, Any> = mutableMapOf()

    private val schemaClassMap: MutableMap<KClass<*>, Schema> = mutableMapOf()

    fun getReuseSchema(modelClass: KClass<*>): Schema? = schemaClassMap[modelClass]

    fun reuse(schemaDef: SchemaDef) = reuse(schemaDef.definition)

    fun reuse(schema: Schema) = reuse(schema.definition)

    fun reuse(referenceObject: ReferenceObject) = reuse(referenceObject.definition)

    private fun reuse(definition: Definition): ReferenceObject {
        when (definition) {
            is HeaderDef -> headers += definition.defPair()
            is ParameterDef -> parameters += definition.defPair()
            is RequestBodiesDef -> requestBodies += definition.defPair()
            is ResponseDef -> responses += definition.defPair()
            is SchemaDef -> schemas += definition.defPair()
            is ExampleDef -> examples += definition.defPair()
        }
        val ref = definition.createRef()
        if (definition is SchemaDef && definition.kClass != null)
            schemaClassMap[definition.kClass] = ref
        return ref
    }
}

// ========== Parameter ==========

enum class ParameterInputType {
    header,
    path,
    query,
    cookie,
    body, // Not supported in OpenAPI v3.
}

open class ParameterDef(
    open val `in`: ParameterInputType,
    val required: Boolean,
    val schema: PropertyDef,
    val description: String? = null,
    val deprecated: Boolean? = null,
    val allowEmptyValue: Boolean? = null,
    val example: Any? = null,
    val examples: Map<String, ExampleDef>? = null
) : Definition(
    "${schema.definition.name}${if (required) "" else "-optional"}"
),
    Parameter {

    @JsonProperty("name")
    open val parameterName: String = schema.definition.name

    @JsonIgnore
    override val type: DefinitionType = DefinitionType.Parameter

    override fun defPair(): Pair<String, ParameterDef> = (refName ?: definition.name) to this

    override fun valuePair(): Pair<String, Parameter> = if (hasRef()) refPair() else defPair()
}

class HeaderDef(
    required: Boolean,
    schema: PropertyDef,
    description: String? = null,
    deprecated: Boolean? = null,
    allowEmptyValue: Boolean? = null,
    example: Any? = null,
    examples: Map<String, ExampleDef>? = null
) : ParameterDef(
    ParameterInputType.header, required, schema, description,
    deprecated, allowEmptyValue, example, examples
), Header {

    @JsonIgnore
    override val parameterName: String = super.parameterName

    @JsonIgnore
    override val `in`: ParameterInputType = super.`in`

    @JsonIgnore
    override val type: DefinitionType = DefinitionType.Header

    override fun defPair(): Pair<String, HeaderDef> = definition.name to this

    override fun valuePair(): Pair<String, Header> = if (hasRef()) refPair() else defPair()
}

// ========== RequestBody ==========

class RequestBodiesDef(
    name: String,
    val content: Map<ContentType, MediaTypeObject>,
    val description: String? = null
) : Definition(name), RequestBodies {

    @JsonIgnore
    override val type: DefinitionType = DefinitionType.RequestBodies

    override fun defPair(): Pair<String, RequestBodiesDef> = definition.name to this

    override fun valuePair(): Pair<String, RequestBodies> = if (hasRef()) refPair() else defPair()
}

// ========== Response ==========

class ResponseDef(
    name: String,
    val description: String,
    @JsonIgnore val statusCode: HttpStatusCode?,
    val content: Map<ContentType, MediaTypeObject>? = null,
    var headers: MutableMap<String, Header> = mutableMapOf(),
) : Definition(name), Response {

    @JsonIgnore
    override val type: DefinitionType = DefinitionType.Response

    override fun defPair(): Pair<String, ResponseDef> = definition.name to this

    override fun valuePair(): Pair<String, Response> = if (hasRef()) refPair() else defPair()
}

// ========== Content ==========

class MediaTypeObject(
    val schema: Schema,
    val example: Any? = null,
    val examples: Map<String, Example>? = null
)

// ========== Example ==========

class ExampleDef(
    name: String,
    val value: Any,
    val summary: String,
    val description: String? = null,
    //val externalValue: String?
) : Definition(name), Example {

    @JsonIgnore
    override val type: DefinitionType = DefinitionType.Example

    override fun defPair(): Pair<String, ExampleDef> = definition.name to this

    override fun valuePair(): Pair<String, Example> = if (hasRef()) refPair() else defPair()
}

// ========== SecuritySchemes ==========

class SecuritySchemeObject(
    val type: String, val `in`: String? = null, val name: String? = null,
    val scheme: String? = null, var openIdConnectUrl: String? = null,
    //var flows:
)
