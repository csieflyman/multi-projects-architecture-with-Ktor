/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.definition

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import fanpoll.infra.utils.IdentifiableObject
import fanpoll.infra.utils.Jackson

// ========== Definition ==========

interface Referenceable {

    val name: String

    val definition: Definition

    fun defPair(): Pair<String, Definition>

    fun refPair(): Pair<String, ReferenceObject>

    fun valuePair(): Pair<String, Referenceable>
}

interface Parameter : Referenceable
interface Header : Parameter
interface RequestBodies : Referenceable
interface Response : Referenceable
interface Schema : Referenceable
interface Example : Referenceable

class ReferenceObject(
    override val name: String,
    override val definition: Definition
) : IdentifiableObject<String>(), Referenceable, Header, Parameter, RequestBodies, Response, Schema, Example {

    val `$ref` = "#/components/${definition.type.fieldName}/${definition.name}"

    override val id: String = name

    override fun defPair(): Pair<String, Definition> = definition.name to definition

    override fun refPair(): Pair<String, ReferenceObject> = name to this

    override fun valuePair(): Pair<String, Referenceable> = refPair()

    @JsonValue
    fun toJson(): JsonNode = Jackson.newObject().put("\$ref", `$ref`)
}

abstract class Definition(
    @JsonIgnore override val name: String,
    @JsonIgnore val refName: String? = null
) : IdentifiableObject<String>(), Referenceable {

    @get:JsonIgnore
    abstract val type: DefinitionType

    @get:JsonIgnore
    override val definition: Definition
        get() = this

    // lazy initialization to avoid reference infinite cycle
    private val refObj: Lazy<ReferenceObject> = lazy {
        ReferenceObject(refName ?: name, this)
    }

    fun hasRef(): Boolean = refObj.isInitialized()

    fun createRef(): ReferenceObject = refObj.value

    fun createRef(refName: String): ReferenceObject = ReferenceObject(refName, this)

    @JsonIgnore
    fun getRef(): ReferenceObject =
        if (refObj.isInitialized()) refObj.value else error("$id referenceObject is not initialized")

    override fun defPair(): Pair<String, Definition> = (refName ?: definition.name) to this

    override fun refPair(): Pair<String, ReferenceObject> = (refName ?: name) to getRef()

    override fun valuePair(): Pair<String, Referenceable> = if (hasRef()) refPair() else defPair()

    @get:JsonIgnore
    override val id: String
        get() = "/${type.fieldName}/$name"
}

enum class DefinitionType(val fieldName: String) {

    Header("headers"), Parameter("parameters"),
    RequestBodies("requestBodies"), Response("responses"),
    Schema("schemas"), Example("examples")
}