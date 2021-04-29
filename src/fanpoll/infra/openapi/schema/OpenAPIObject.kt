/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.schema

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import fanpoll.infra.openapi.schema.component.definitions.ComponentsObject
import fanpoll.infra.openapi.schema.component.support.SecurityScheme
import fanpoll.infra.openapi.schema.operation.definitions.OperationObject
import fanpoll.infra.utils.Jackson
import io.ktor.http.HttpMethod

class OpenAPIObject(
    val openapi: String = "3.0.3",
    val info: Info,
    val servers: List<Server>,
    val tags: Set<Tag>,
    //@JsonIgnore val security: List<Security>
) {
    lateinit var components: ComponentsObject
        private set
    val paths: PathsObject = PathsObject()

    fun initComponents(securitySchemes: List<SecurityScheme>) {
        components = ComponentsObject(securitySchemes)
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

class PathsObject {

    private val pathItems: MutableMap<String, MutableMap<String, OperationObject>> = mutableMapOf()

    fun addPath(path: String, method: HttpMethod, operation: OperationObject) {
        pathItems.getOrPut(path) { mutableMapOf() }[method.value.toLowerCase()] = operation
    }

    @JsonValue
    fun toJson(): JsonNode {
        return Jackson.toJson(pathItems)
    }
}