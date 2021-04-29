/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.schema.operation.definitions

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
}