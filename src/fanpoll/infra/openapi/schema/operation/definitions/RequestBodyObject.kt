/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.schema.operation.definitions

import com.fasterxml.jackson.databind.JsonNode
import fanpoll.infra.openapi.schema.operation.support.Definition
import fanpoll.infra.openapi.schema.operation.support.RequestBody
import io.ktor.http.ContentType

class RequestBodyObject(
    name: String,
    val content: Map<ContentType, MediaTypeObject>,
    val description: String? = null
) : Definition(name), RequestBody {

    override fun componentsFieldName(): String = "requestBodies"

    override fun defPair(): Pair<String, RequestBodyObject> = name to this

    override fun valuePair(): Pair<String, RequestBody> = if (hasRef()) refPair() else defPair()

    fun addJsonExample(json: JsonNode) {
        content[ContentType.Application.Json]!!.example = json
    }

    fun addJsonExample(example: ExampleObject) {
        if (content[ContentType.Application.Json]!!.examples == null)
            content[ContentType.Application.Json]!!.examples = mutableMapOf()
        content[ContentType.Application.Json]!!.examples!!.plusAssign((example.name to example))
    }
}