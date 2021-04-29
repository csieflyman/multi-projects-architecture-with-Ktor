/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.schema.operation.support

import com.fasterxml.jackson.annotation.JsonIgnore
import fanpoll.infra.openapi.schema.operation.definitions.ReferenceObject

abstract class Definition(
    @JsonIgnore override val name: String,
    @JsonIgnore val refName: String? = null
) : Element {

    abstract fun componentsFieldName(): String

    @JsonIgnore
    override fun getId(): String = "/${componentsFieldName()}/$name"

    @JsonIgnore
    override fun getDefinition(): Definition = this

    @JsonIgnore
    override fun getReference(): ReferenceObject =
        if (refObj.isInitialized()) refObj.value
        else error("${getId()} referenceObject is not initialized")

    // lazy initialization to avoid reference infinite cycle
    private val refObj: Lazy<ReferenceObject> = lazy {
        ReferenceObject(refName ?: name, this)
    }

    fun hasRef(): Boolean = refObj.isInitialized()

    fun createRef(): ReferenceObject = refObj.value

    fun createRef(refName: String): ReferenceObject = ReferenceObject(refName, this)

    fun refPair(): Pair<String, ReferenceObject> = name to getReference()

    abstract fun defPair(): Pair<String, Definition>

    abstract fun valuePair(): Pair<String, Element>
}