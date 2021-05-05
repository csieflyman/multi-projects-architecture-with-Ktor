/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.schema.operation.definitions

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonIgnore
import fanpoll.infra.ResponseCode
import fanpoll.infra.openapi.schema.component.support.BuiltinComponents
import fanpoll.infra.openapi.schema.component.support.BuiltinComponents.ErrorResponseSchema
import fanpoll.infra.openapi.schema.operation.support.Parameter
import fanpoll.infra.openapi.schema.operation.support.RequestBody
import fanpoll.infra.openapi.schema.operation.support.Response
import fanpoll.infra.openapi.schema.operation.support.utils.ResponseUtils
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode

class OperationObject(
    val operationId: String,
    val tags: List<String>,
    var summary: String = operationId,
    var description: String? = null,
    val deprecated: Boolean? = null
) {

    // Multiple Authentication Types: listOf(listOf(A, B), listOf(C, D)) => (A AND B) OR (C AND D)
    @JsonIgnore
    var security: List<List<SecurityRequirementObject>>? = null

    @JsonGetter("security")
    fun toJsonSecurity(): List<Map<String, List<String>>>? {
        return security?.map { s1 -> s1.associate { s2 -> s2.scheme.name to s2.scopes } }
    }

    @JsonIgnore
    val parameters: MutableList<Parameter> = mutableListOf()

    @JsonGetter("parameters")
    fun toJsonParameters(): List<Parameter> = parameters.sortedBy { (it.getDefinition() as ParameterObject).`in`.ordinal }

    var requestBody: RequestBody? = null

    @JsonIgnore
    private val responses: MutableMap<HttpStatusCode, Response> = mutableMapOf()

    @JsonIgnore
    var defaultResponse: Response = BuiltinComponents.DefaultErrorResponse

    @JsonGetter("responses")
    fun toJsonResponses(): Map<String, Response> {
        val result = responses.toSortedMap(compareBy { it.value }).mapKeys { it.key.value.toString() }.toMutableMap()
        result["default"] = defaultResponse
        return result
    }

    // use "oneOf" schema for varying responseBody formats of responseCode
    fun addSuccessResponse(response: Response) {
        responses[(response.getDefinition() as ResponseObject).statusCode!!] = response
    }

    fun addErrorResponses(vararg responseCodes: ResponseCode) {
        responses += responseCodes.toList().groupBy { it.httpStatusCode }
            .mapValues {
                ResponseObject(
                    "${it.key.value}-ErrorResponse",
                    ResponseUtils.buildResponseCodesDescription(it.value),
                    it.key, mapOf(ContentType.Application.Json to MediaTypeObject(ErrorResponseSchema))
                )
            }
    }
}