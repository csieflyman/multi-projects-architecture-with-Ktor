/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

@file:OptIn(KtorExperimentalLocationsAPI::class)

package fanpoll.infra.openapi.definition

import fanpoll.infra.ResponseCode
import fanpoll.infra.openapi.OpenApiConfig
import fanpoll.infra.openapi.definition.DefaultReusableComponents.EmptyBodyResponse
import fanpoll.infra.openapi.definition.DefaultReusableComponents.FreeFormDataResponse
import fanpoll.infra.openapi.definition.DefaultReusableComponents.LongIdResponse
import fanpoll.infra.openapi.definition.DefaultReusableComponents.ResponseCodeValueSchema
import fanpoll.infra.openapi.definition.DefaultReusableComponents.StringIdResponse
import fanpoll.infra.openapi.definition.DefaultReusableComponents.UUIDResponse
import fanpoll.infra.openapi.definition.SchemaUtils.getModelName
import fanpoll.infra.openapi.definition.SchemaUtils.toModelDef
import fanpoll.infra.openapi.definition.SchemaUtils.toPropertyDef
import fanpoll.infra.utils.I18nUtils
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import mu.KotlinLogging
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.typeOf

object ComponentsUtils {

    private val logger = KotlinLogging.logger {}

    private const val SWAGGER_NEWLINE = "<br><br>"

    fun getLocationPath(locationClass: KClass<*>): String {
        val location = locationClass.annotations.first { it.annotationClass == Location::class } as? Location
            ?: error("[OpenAPI]: Location Class ${locationClass.qualifiedName} @Location annotation is required")
        return location.path
    }

    @KtorExperimentalLocationsAPI
    fun createParameterDefsFromLocation(locationClass: KClass<*>): List<ParameterDef> {
        val annotation = locationClass.annotations.first { it.annotationClass == Location::class } as? Location
            ?: error("[OpenAPI]: Location Class ${locationClass.qualifiedName} @Location annotation is required")

        val pathParameters = annotation.path.split("/").filter { it.startsWith("{") && it.endsWith("}") }
            .map { it.substring(1, it.length - 1) }.toSet()

        return locationClass.primaryConstructor!!.parameters.map { kParameter ->
            val propertyName = kParameter.name!!
            val propertyDef = toPropertyDef(propertyName, kParameter.type.classifier as KClass<*>)
                ?: error("location ${locationClass.qualifiedName} property $propertyName cannot map to PropertyDef")
            if (pathParameters.contains(propertyName)) {
                ParameterDef(ParameterInputType.path, true, propertyDef)
            } else {
                ParameterDef(ParameterInputType.query, !kParameter.isOptional, propertyDef)
            }
        }
    }

    fun createSchema(components: Components, schemaKType: KType, modelName: String? = null): Schema =
        toModelDef(components, modelName ?: getModelName(schemaKType), schemaKType)

    fun createRequestBodiesDef(components: Components, requestBodyType: KType): RequestBodiesDef {
        val modelName = getModelName(requestBodyType)
        return RequestBodiesDef(
            "$modelName-RequestBody",
            mapOf(
                ContentType.Application.Json to
                        MediaTypeObject(schema = createSchema(components, requestBodyType, modelName))
            )
        )
    }

    private val UnitKType = typeOf<Unit>()
    private val AnyKType = typeOf<Any>()
    private val LongIdKType = typeOf<Long>()
    private val StringIdKType = typeOf<String>()
    private val UUIDIdKType = typeOf<UUID>()

    fun createResponse(components: Components, responseBodyType: KType, responseCode: ResponseCode? = null): Response {
        return if (responseCode == null) {
            when (responseBodyType) {
                UnitKType -> EmptyBodyResponse
                LongIdKType -> LongIdResponse
                StringIdKType -> StringIdResponse
                UUIDIdKType -> UUIDResponse
                AnyKType -> FreeFormDataResponse
                else -> createDataResponse(createSchema(components, responseBodyType))
            }
        } else {
            ResponseDef(
                responseCode.name + "-Response", buildResponseCodeDescription(responseCode), responseCode.httpStatusCode,
                mapOf(
                    ContentType.Application.Json to
                            MediaTypeObject(schema = createSchema(components, responseBodyType))
                )
            )
        }
    }

    fun createDataResponse(dataSchema: Schema): ResponseDef {
        val requiredProperties = listOf("code", "data")
        val properties: Map<String, Schema> = listOf(
            "code" to ResponseCodeValueSchema,
            "data" to dataSchema,
        ).toMap()

        val modelDef = ModelDef(dataSchema.name + "-Data", requiredProperties, properties)
        properties.forEach { (it.value.definition as SchemaDef).parent = modelDef }

        return ResponseDef(
            dataSchema.name + "-Response", "Success data response", HttpStatusCode.OK,
            mapOf(ContentType.Application.Json to MediaTypeObject(modelDef))
        )
    }

    fun createErrorResponseDef(responseCodes: Set<ResponseCode>, schema: ModelDef): ResponseDef {
        val sortedCodes = responseCodes.sortedBy { it.value.toIntOrNull() ?: Int.MAX_VALUE }
        return ResponseDef(
            schema.name + "-Response", buildResponseCodesDescription(sortedCodes), null,
            mapOf(ContentType.Application.Json to MediaTypeObject(schema))
        )
    }

    fun buildResponseCodesDescription(responseCodes: List<ResponseCode>): String {
        return responseCodes.groupBy { it.codeType }.mapValues { entry ->
            entry.value.joinToString(SWAGGER_NEWLINE) { buildResponseCodeDescription(it) }
        }.map { "[${it.key.name}] $SWAGGER_NEWLINE ${it.value}" }.joinToString(SWAGGER_NEWLINE)
    }

    private fun buildResponseCodeDescription(code: ResponseCode): String {
        return "${code.value} => ${code.name} (${code.httpStatusCode.value}) ${
            I18nUtils.getCodeMessageOrNull(code, null, OpenApiConfig.langCode) ?: ""
        }"
    }
}