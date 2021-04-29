/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

@file:OptIn(KtorExperimentalLocationsAPI::class)

package fanpoll.infra.openapi

import fanpoll.infra.auth.PrincipalAuth
import fanpoll.infra.database.DynamicDBQuery
import fanpoll.infra.openapi.schema.Tag
import fanpoll.infra.openapi.schema.component.definitions.ComponentsObject
import fanpoll.infra.openapi.schema.component.support.BuiltinComponents
import fanpoll.infra.openapi.schema.operation.definitions.OneOfSchema
import fanpoll.infra.openapi.schema.operation.definitions.OperationObject
import fanpoll.infra.openapi.schema.operation.support.Schema
import fanpoll.infra.openapi.schema.operation.support.converters.ParameterObjectConverter
import fanpoll.infra.openapi.schema.operation.support.converters.RequestBodyObjectConverter
import fanpoll.infra.openapi.schema.operation.support.converters.ResponseObjectConverter
import fanpoll.infra.openapi.schema.operation.support.converters.SchemaObjectConverter
import fanpoll.infra.utils.IdentifiableObject
import io.ktor.http.HttpMethod
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import mu.KotlinLogging
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf

class OpenApiOperation(
    override val id: String,
    val tags: List<Tag>,
    private val notYetImplemented: Boolean = false,
    private val configure: (OperationObject.() -> Unit)? = null
) : IdentifiableObject<String>() {

    lateinit var projectOpenApi: ProjectOpenApi
        private set
    lateinit var operationObject: OperationObject
        private set

    companion object {
        private val logger = KotlinLogging.logger {}
        val kType = typeOf<OpenApiOperation>()
        private val dynamicDBQueryKType = typeOf<DynamicDBQuery<*>>()
    }

    fun init(projectOpenApi: ProjectOpenApi) {
        this.projectOpenApi = projectOpenApi
        operationObject = OperationObject(operationId = id, tags = tags.map { it.name }, summary = id)
    }

    fun bindRoute(
        routeAuths: List<PrincipalAuth>?, routePath: String, method: HttpMethod,
        requestBodyType: KType, responseBodyType: KType,
        locationClass: KClass<*>? = null
    ) {
        bindPath(routePath, method, locationClass)
        bindAuth(routeAuths)
        bindOperation(locationClass, requestBodyType, responseBodyType)
    }

    private fun bindPath(routePath: String, method: HttpMethod, locationClass: KClass<*>? = null) {
        var path = routePath

        if (locationClass != null) {
            path += getKtorLocationPath(locationClass)
        }
        projectOpenApi.openAPIObject.paths.addPath(path, method, operationObject)
    }

    private fun getKtorLocationPath(locationClass: KClass<*>): String {
        val location = locationClass.annotations.first { it.annotationClass == Location::class } as? Location
            ?: error("[OpenAPI]: Location Class ${locationClass.qualifiedName} @Location annotation is required")
        return location.path
    }

    private fun bindAuth(routeAuths: List<PrincipalAuth>?) {
        logger.debug { "OpenAPI $id bindAuth => $routeAuths" }
        operationObject.summary += "　=>　Auth = [${routeAuths?.joinToString(" || ") ?: "Public"}]"
        if (routeAuths != null) {
            setOperationSecurities(routeAuths)
            setClientVersionHeader(routeAuths)
        }
    }

    private fun setOperationSecurities(routeAuths: List<PrincipalAuth>) {
        val securitySchemes = routeAuths.map { routeAuth ->
            routeAuth.securitySchemes.map { it.createSecurity() }
        }.filter { it.isNotEmpty() }.toSet()
        if (securitySchemes.isNotEmpty()) {
            operationObject.security = securitySchemes.toList()
        }
    }

    private fun setClientVersionHeader(routeAuths: List<PrincipalAuth>) {
        if (routeAuths.flatMap { auth ->
                when (auth) {
                    is PrincipalAuth.Service -> auth.sourceRoleMap.keys
                    is PrincipalAuth.User -> auth.allowSources
                }
            }.any { it.login && it.userDeviceType?.isApp() == true }) {
            operationObject.parameters += BuiltinComponents.ClientVersionOptionalHeader
        }
    }

    private fun bindOperation(
        locationClass: KClass<*>? = null,
        requestBodyType: KType,
        responseBodyType: KType
    ) {
        logger.debug {
            "OpenAPI $id bindOperation => " +
                    "locationClass = ${locationClass?.qualifiedName}, " +
                    "requestBodyType = $requestBodyType, responseBodyType = $responseBodyType"
        }

        bindOperationClass(locationClass, requestBodyType, responseBodyType)

        configure?.invoke(operationObject)

        if (notYetImplemented)
            operationObject.summary = "[NYI] ${operationObject.summary}"
    }

    private fun bindOperationClass(
        locationClass: KClass<*>? = null,
        requestBodyType: KType,
        responseBodyType: KType
    ) {
        val components = projectOpenApi.openAPIObject.components
        if (locationClass != null) {
            operationObject.parameters += ParameterObjectConverter.toParameter(locationClass)
        }

        if (requestBodyType != typeOf<Unit>()) {
            operationObject.requestBody = RequestBodyObjectConverter.toRequestBody(components, requestBodyType)
        }

        if (responseBodyType.isSubtypeOf(dynamicDBQueryKType)) {
            val dtoSchema = SchemaObjectConverter.toSchema(components, responseBodyType.arguments[0].type!!)
            val dtoSchemaRef = components.add(dtoSchema.getDefinition())
            val dynamicQuerySchemas = buildDynamicQueryResponseSchemas(dtoSchemaRef, components)
            operationObject.addSuccessResponses(BuiltinComponents.buildDynamicQueryResponse(dynamicQuerySchemas))

            operationObject.parameters += BuiltinComponents.DynamicQueryParameters
        } else operationObject.addSuccessResponses(ResponseObjectConverter.toResponse(components, responseBodyType))
    }

    private fun buildDynamicQueryResponseSchemas(itemSchema: Schema, components: ComponentsObject): OneOfSchema {
        val pagingResponseSchema = BuiltinComponents.buildDynamicQueryPagingResponseSchema(itemSchema)
        val itemsResponseSchema = BuiltinComponents.buildDynamicQueryItemsResponseSchema(itemSchema)
        components.add(pagingResponseSchema)
        components.add(itemsResponseSchema)
        return OneOfSchema(itemSchema.name, listOf(
            pagingResponseSchema,
            itemsResponseSchema,
            BuiltinComponents.DynamicQueryTotalResponseSchema
        ).map { it.createRef() })
    }
}