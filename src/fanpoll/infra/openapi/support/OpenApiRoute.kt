/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

@file:OptIn(KtorExperimentalLocationsAPI::class)

package fanpoll.infra.openapi.support

import fanpoll.infra.auth.PrincipalAuth
import fanpoll.infra.database.DynamicDBQuery
import fanpoll.infra.openapi.definition.*
import fanpoll.infra.utils.IdentifiableObject
import io.ktor.http.HttpMethod
import io.ktor.locations.KtorExperimentalLocationsAPI
import mu.KotlinLogging
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf

class OpenApiRoute(
    override val id: String,
    val tags: List<Tag>,
    private val notYetImplemented: Boolean = false,
    private val configure: (Operation.() -> Unit)? = null
) : IdentifiableObject<String>() {

    lateinit var openApi: OpenApi
        private set
    lateinit var operation: Operation
        private set

    companion object {
        private val logger = KotlinLogging.logger {}
        private val dynamicDBQueryKType = typeOf<DynamicDBQuery<*>>()
    }

    fun init(openApi: OpenApi) {
        this.openApi = openApi
        operation = Operation(operationId = id, tags = tags.map { it.name }, summary = id)
    }

    fun bindAuth(routeAuths: List<PrincipalAuth>?) {
        logger.debug { "OpenAPI $id bindAuth => $routeAuths" }
        operation.summary += "　=>　Auth = [${routeAuths?.joinToString(" || ") ?: "Public"}]"
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
            operation.security = securitySchemes.toList()
        }
    }

    private fun setClientVersionHeader(routeAuths: List<PrincipalAuth>) {
        if (routeAuths.flatMap { auth ->
                when (auth) {
                    is PrincipalAuth.Service -> auth.sourceRoleMap.keys
                    is PrincipalAuth.User -> auth.allowSources
                }
            }.any { it.login && it.userDeviceType?.isApp() == true }) {
            operation.parameters += DefaultReusableComponents.ClientVersionOptionalHeader
        }
    }

    fun bindOperation(
        locationClass: KClass<*>? = null,
        requestBodyType: KType,
        responseBodyType: KType
    ) {
        logger.debug {
            "OpenAPI $id bindOperation => " +
                    "locationClass = ${locationClass?.qualifiedName}, " +
                    "requestBodyType = $requestBodyType, responseBodyType = $responseBodyType"
        }

        bindClass(locationClass, requestBodyType, responseBodyType)

        configure?.invoke(operation)

        if (notYetImplemented)
            operation.summary = "[NYI] ${operation.summary}"
    }

    private fun bindClass(
        locationClass: KClass<*>? = null,
        requestBodyType: KType,
        responseBodyType: KType
    ) {
        val components = openApi.root.components
        if (locationClass != null) {
            operation.parameters += ComponentsUtils.createParameterDefsFromLocation(locationClass)
        }

        if (requestBodyType != OpenApiRouteSupport.UnitKType) {
            operation.requestBody = ComponentsUtils.createRequestBodiesDef(components, requestBodyType)
        }

        if (responseBodyType.isSubtypeOf(dynamicDBQueryKType)) {
            val dtoSchema = ComponentsUtils.createSchema(components, responseBodyType.arguments[0].type!!)
            val dtoSchemaRef = components.reuse(dtoSchema)
            val dynamicQuerySchemas = buildDynamicQueryResponseSchemas(dtoSchemaRef, components)
            operation.addSuccessResponses(DefaultReusableComponents.buildDynamicQueryResponse(dynamicQuerySchemas))

            operation.parameters += DefaultReusableComponents.DynamicQueryParameters
        } else operation.addSuccessResponses(ComponentsUtils.createResponse(components, responseBodyType))
    }

    private fun buildDynamicQueryResponseSchemas(itemSchema: Schema, components: Components): OneOfSchema {
        val pagingResponseSchema = DefaultReusableComponents.buildDynamicQueryPagingResponseSchema(itemSchema)
        val itemsResponseSchema = DefaultReusableComponents.buildDynamicQueryItemsResponseSchema(itemSchema)
        components.reuse(pagingResponseSchema)
        components.reuse(itemsResponseSchema)
        return OneOfSchema(itemSchema.name, listOf(
            pagingResponseSchema,
            itemsResponseSchema,
            DefaultReusableComponents.DynamicQueryTotalResponseSchema
        ).map { it.createRef() })
    }

    fun bindPath(routePath: String, method: HttpMethod, locationClass: KClass<*>? = null) {
        var path = routePath

        if (locationClass != null) {
            path += ComponentsUtils.getLocationPath(locationClass)
        }
        openApi.root.paths.addPath(path, method, operation)
    }
}