/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.schema.operation.support.converters

import fanpoll.infra.base.response.ResponseCode
import fanpoll.infra.openapi.schema.component.definitions.ComponentsObject
import fanpoll.infra.openapi.schema.component.support.BuiltinComponents
import fanpoll.infra.openapi.schema.operation.definitions.MediaTypeObject
import fanpoll.infra.openapi.schema.operation.definitions.ModelDef
import fanpoll.infra.openapi.schema.operation.definitions.ResponseObject
import fanpoll.infra.openapi.schema.operation.definitions.SchemaObject
import fanpoll.infra.openapi.schema.operation.support.Response
import fanpoll.infra.openapi.schema.operation.support.Schema
import fanpoll.infra.openapi.schema.operation.support.utils.DataModelUtils.AnyKType
import fanpoll.infra.openapi.schema.operation.support.utils.DataModelUtils.LongIdKType
import fanpoll.infra.openapi.schema.operation.support.utils.DataModelUtils.StringIdKType
import fanpoll.infra.openapi.schema.operation.support.utils.DataModelUtils.UUIDIdKType
import fanpoll.infra.openapi.schema.operation.support.utils.DataModelUtils.UnitKType
import fanpoll.infra.openapi.schema.operation.support.utils.ResponseUtils.buildResponseCodeDescription
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlin.reflect.KType

object ResponseObjectConverter {

    fun toResponse(components: ComponentsObject, modelKType: KType, responseCode: ResponseCode? = null): Response {
        return if (responseCode == null) {
            when (modelKType) {
                UnitKType -> BuiltinComponents.EmptyBodyResponse
                LongIdKType -> BuiltinComponents.LongIdResponse
                StringIdKType -> BuiltinComponents.StringIdResponse
                UUIDIdKType -> BuiltinComponents.UUIDResponse
                AnyKType -> BuiltinComponents.FreeFormDataResponse
                else -> toDataResponse(SchemaObjectConverter.toSchema(components, modelKType))
            }
        } else {
            ResponseObject(
                responseCode.name + "-Response", buildResponseCodeDescription(responseCode), responseCode.httpStatusCode,
                mapOf(
                    ContentType.Application.Json to
                            MediaTypeObject(schema = SchemaObjectConverter.toSchema(components, modelKType))
                )
            )
        }
    }

    fun toDataResponse(dataSchema: Schema): ResponseObject {
        val requiredProperties = listOf("code", "data")
        val properties: Map<String, Schema> = listOf(
            "code" to BuiltinComponents.ResponseCodeSchema,
            "data" to dataSchema,
        ).toMap()

        val modelDef = ModelDef(dataSchema.name + "-Data", requiredProperties, properties)
        properties.forEach { (it.value.getDefinition() as SchemaObject).parent = modelDef }

        return ResponseObject(
            dataSchema.name + "-Response", "Success", HttpStatusCode.OK,
            mapOf(ContentType.Application.Json to MediaTypeObject(modelDef))
        )
    }
}