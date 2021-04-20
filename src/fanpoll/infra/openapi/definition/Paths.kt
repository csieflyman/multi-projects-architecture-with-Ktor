/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.definition

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import fanpoll.infra.ResponseCode
import fanpoll.infra.openapi.definition.ComponentsUtils.buildResponseCodesDescription
import fanpoll.infra.utils.Jackson
import io.ktor.http.HttpMethod

class Paths {

    private val pathItems: MutableMap<String, MutableMap<String, Operation>> = mutableMapOf()

    fun addPath(path: String, method: HttpMethod, operation: Operation) {
        pathItems.getOrPut(path) { mutableMapOf() }[method.value.toLowerCase()] = operation
    }

    @JsonValue
    fun toJson(): JsonNode {
        return Jackson.toJson(pathItems)
    }
}

class Operation(
    var operationId: String? = null,
    var tags: List<String>? = null,
    var summary: String? = null,
    var description: String? = null,
    @JsonIgnore val parameters: MutableList<Parameter> = mutableListOf(),
    var requestBody: RequestBodies? = null,
    // Multiple Authentication Types: listOf(listOf(A, B), listOf(C, D)) => (A AND B) OR (C AND D)
    @JsonIgnore var security: List<List<Security>>? = null,
    var deprecated: Boolean? = null
) {

    @JsonIgnore
    private val responses: MutableMap<String, Response> = mutableMapOf()

    @JsonGetter("security")
    fun toJsonSecurity(): List<Map<String, List<String>>>? {
        return security?.map { s1 -> s1.map { s2 -> s2.scheme.name to s2.scopes }.toMap() }
    }

    @JsonGetter("parameters")
    fun toJsonParameters(): List<Parameter> = parameters.sortedBy { (it.definition as ParameterDef).`in`.ordinal }

    @JsonGetter("responses")
    fun toJsonResponses(): Map<String, Response> = responses.toSortedMap(compareBy { it.toIntOrNull() ?: Int.MAX_VALUE })

    // use "oneOf" schema for varying responseBody formats of responseCode
    fun addSuccessResponses(vararg responses: Response) {
        responses.forEach {
            val statusCode = (it.definition as ResponseDef).statusCode!!.value.toString()
            require(!this.responses.containsKey(statusCode))
            this.responses[statusCode] = it
        }
    }

    fun addSuccessResponses(responseCodes: Set<ResponseCode>) {
        responses += responseCodes.toList().groupBy { it.httpStatusCode }
            .mapValues {
                ResponseDef("${it.key.value}-Response", buildResponseCodesDescription(it.value), it.key, null)
            }.mapKeys { it.key.toString() }
    }

    fun setSuccessResponse(responseCode: ResponseCode, response: Response) {
        responses[responseCode.httpStatusCode.value.toString()] = response
    }

    fun setErrorResponse(response: Response) {
        require(!responses.containsKey("default"))
        responses["default"] = response
    }

    fun setSuccessResponseHeader(headers: List<Header>, responseCode: ResponseCode? = null) {
        if (responseCode == null) {
            responses.filterKeys { it != "default" }.values.forEach {
                setResponseHeader(it, headers)
            }
        } else {
            setResponseHeader(getSuccessResponse(responseCode), headers)
        }
    }

    fun setErrorResponseHeader(headers: List<Header>) {
        setResponseHeader(getErrorResponse(), headers)
    }

    private fun setResponseHeader(response: Response, headers: List<Header>) {
        require(response !is ReferenceObject)
        (response as ResponseDef).headers.plusAssign(headers.map { it.name to it })
    }

    private fun getSuccessResponse(responseCode: ResponseCode): Response {
        return responses[responseCode.httpStatusCode.value.toString()]!!
    }

    private fun getErrorResponse(): Response {
        return responses["default"]!!
    }
}