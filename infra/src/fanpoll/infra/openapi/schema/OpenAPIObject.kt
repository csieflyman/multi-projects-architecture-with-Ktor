/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.schema

import com.fasterxml.jackson.annotation.JsonProperty
import fanpoll.infra.openapi.schema.component.definitions.ComponentsObject
import fanpoll.infra.openapi.schema.operation.definitions.OperationObject

class OpenAPIObject(
    val openapi: String = "3.0.3",
    val info: Info,
    val servers: List<Server>,
    val tags: MutableList<Tag>
) {
    val components: ComponentsObject = ComponentsObject()

    @JsonProperty("paths")
    private val paths: MutableMap<String, MutableMap<String, OperationObject>> = mutableMapOf()

    fun addPath(path: String, method: String, operation: OperationObject) {
        paths.getOrPut(path) { mutableMapOf() }[method] = operation
    }

    fun complete() {
        val usedTags = paths.values.flatMap { it.values.flatMap { operation -> operation.tags } }.toSet()
        tags.removeIf { !usedTags.contains(it.name) }
    }
}

class Info(
    val title: String,
    val description: String,
    val version: String,
    val contact: Contact? = null
)

class Contact(
    val name: String? = null,
    val url: String? = null,
    val email: String? = null
)

class Server(
    val url: String,
    val description: String? = null
)

class Tag(
    val name: String,
    val description: String? = null,
    val externalDocs: String? = null
)