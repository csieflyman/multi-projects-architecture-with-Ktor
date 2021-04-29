/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.schema.operation.definitions

import com.fasterxml.jackson.annotation.JsonIgnore
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
}