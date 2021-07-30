/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.schema.operation.definitions

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import fanpoll.infra.openapi.schema.operation.support.Definition
import fanpoll.infra.openapi.schema.operation.support.Header
import fanpoll.infra.openapi.schema.operation.support.Response
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode

class ResponseObject(
    name: String,
    val description: String,
    @JsonIgnore val statusCode: HttpStatusCode?,
    val content: Map<ContentType, MediaTypeObject>? = null,
    var headers: MutableMap<String, Header> = mutableMapOf(),
) : Definition(name), Response {

    override fun componentsFieldName(): String = "responses"

    override fun defPair(): Pair<String, ResponseObject> = name to this

    override fun valuePair(): Pair<String, Response> = if (hasRef()) refPair() else defPair()

    fun addJsonExample(json: JsonNode) {
        content!![ContentType.Application.Json]!!.example = json
    }

    fun addJsonExample(example: ExampleObject) {
        if (content!![ContentType.Application.Json]!!.examples == null)
            content[ContentType.Application.Json]!!.examples = mutableMapOf()
        content[ContentType.Application.Json]!!.examples!!.plusAssign((example.name to example))
    }
}