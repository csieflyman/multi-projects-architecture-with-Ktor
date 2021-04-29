/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.schema.operation.definitions

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonIgnore
import fanpoll.infra.ResponseCode
import fanpoll.infra.openapi.schema.operation.support.Header
import fanpoll.infra.openapi.schema.operation.support.Parameter
import fanpoll.infra.openapi.schema.operation.support.RequestBody
import fanpoll.infra.openapi.schema.operation.support.Response
import fanpoll.infra.openapi.schema.operation.support.utils.ResponseUtils

class OperationObject(
    var operationId: String? = null,
    var tags: List<String>? = null,
    var summary: String? = null,
    var description: String? = null,
    @JsonIgnore val parameters: MutableList<Parameter> = mutableListOf(),
    var requestBody: RequestBody? = null,
    // Multiple Authentication Types: listOf(listOf(A, B), listOf(C, D)) => (A AND B) OR (C AND D)
    @JsonIgnore var security: List<List<SecurityRequirementObject>>? = null,
    var deprecated: Boolean? = null
) {

    @JsonIgnore
    private val responses: MutableMap<String, Response> = mutableMapOf()

    @JsonGetter("security")
    fun toJsonSecurity(): List<Map<String, List<String>>>? {
        return security?.map { s1 -> s1.associate { s2 -> s2.scheme.name to s2.scopes } }
    }

    @JsonGetter("parameters")
    fun toJsonParameters(): List<Parameter> = parameters.sortedBy { (it.getDefinition() as ParameterObject).`in`.ordinal }

    @JsonGetter("responses")
    fun toJsonResponses(): Map<String, Response> = responses.toSortedMap(compareBy { it.toIntOrNull() ?: Int.MAX_VALUE })

    // use "oneOf" schema for varying responseBody formats of responseCode
    fun addSuccessResponses(vararg responses: Response) {
        responses.forEach {
            val statusCode = (it.getDefinition() as ResponseObject).statusCode!!.value.toString()
            require(!this.responses.containsKey(statusCode))
            this.responses[statusCode] = it
        }
    }

    fun addSuccessResponses(responseCodes: Set<ResponseCode>) {
        responses += responseCodes.toList().groupBy { it.httpStatusCode }
            .mapValues {
                ResponseObject(
                    "${it.key.value}-Response",
                    ResponseUtils.buildResponseCodesDescription(it.value), it.key, null
                )
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
        (response as ResponseObject).headers.plusAssign(headers.map { it.name to it })
    }

    private fun getSuccessResponse(responseCode: ResponseCode): Response {
        return responses[responseCode.httpStatusCode.value.toString()]!!
    }

    private fun getErrorResponse(): Response {
        return responses["default"]!!
    }
}