/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

@file:OptIn(KtorExperimentalLocationsAPI::class)

package fanpoll.infra.openapi

import fanpoll.infra.auth.AuthorizedRoutePrincipalAuthsKey
import fanpoll.infra.auth.PrincipalAuth
import fanpoll.infra.base.query.DynamicQueryLocation
import fanpoll.infra.base.util.IdentifiableObject
import fanpoll.infra.openapi.schema.OpenAPIObject
import fanpoll.infra.openapi.schema.Tag
import fanpoll.infra.openapi.schema.component.support.BuiltinComponents
import fanpoll.infra.openapi.schema.operation.definitions.OperationObject
import fanpoll.infra.openapi.schema.operation.support.converters.ParameterObjectConverter
import fanpoll.infra.openapi.schema.operation.support.converters.RequestBodyObjectConverter
import fanpoll.infra.openapi.schema.operation.support.converters.ResponseObjectConverter
import io.ktor.http.HttpMethod
import io.ktor.server.auth.AuthenticationRouteSelector
import io.ktor.server.locations.KtorExperimentalLocationsAPI
import io.ktor.server.locations.Location
import io.ktor.server.routing.PathSegmentConstantRouteSelector
import io.ktor.server.routing.PathSegmentParameterRouteSelector
import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import mu.KotlinLogging
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class OpenApiOperation(
    override val id: String,
    val tags: List<Tag>,
    private val notYetImplemented: Boolean = false,
    deprecated: Boolean = false,
    private val configure: (OperationObject.() -> Unit)? = null
) : IdentifiableObject<String>() {

    private val operationObject = OperationObject(id, tags.map { it.name }, deprecated = deprecated)
    private lateinit var openAPIObject: OpenAPIObject

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    fun init(openAPIObject: OpenAPIObject) {
        this.openAPIObject = openAPIObject
    }

    fun bindRoute(
        route: Route, path: String? = null, method: HttpMethod,
        requestBodyType: KType, responseBodyType: KType, locationClass: KClass<*>? = null
    ) {
        bindPath(routePath(route, path), method, locationClass)
        bindAuth(routeAuth(route))
        bindOperation(locationClass, requestBodyType, responseBodyType)
    }

    private fun bindPath(routePath: String, method: HttpMethod, locationClass: KClass<*>? = null) {
        var path = routePath

        if (locationClass != null) {
            path += getKtorLocationPath(locationClass)
        }
        openAPIObject.addPath(path, method, operationObject)
    }

    @OptIn(KtorExperimentalLocationsAPI::class)
    private fun getKtorLocationPath(locationClass: KClass<*>): String {
        val location = locationClass.annotations.first { it.annotationClass == Location::class } as? Location
            ?: error("[OpenAPI]: Location Class ${locationClass.qualifiedName} @Location annotation is required")
        return location.path
    }

    private fun bindAuth(routeAuths: List<PrincipalAuth>?) {
        logger.debug { "OpenAPI $id bindAuth => $routeAuths" }
        operationObject.summary += "　Auth => (${routeAuths?.joinToString(" or ") ?: "Public"})"
        if (routeAuths != null) {
            setOperationSecurities(routeAuths)
            setSessionIdHeader(routeAuths)
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

    private fun setSessionIdHeader(routeAuths: List<PrincipalAuth>) {
        if (routeAuths.all { it is PrincipalAuth.User }) {
            operationObject.parameters += BuiltinComponents.SessionIdHeader
        } else if (routeAuths.any { it is PrincipalAuth.User }) {
            operationObject.parameters += BuiltinComponents.SessionIdOptionalHeader
        }
    }

    private fun setClientVersionHeader(routeAuths: List<PrincipalAuth>) {
        if (routeAuths.flatMap { it.allowSources }.any { it.login && it.type.isApp() }) {
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
        val components = openAPIObject.components

        if (locationClass != null) {
            if (locationClass == DynamicQueryLocation::class)
                operationObject.parameters += BuiltinComponents.DynamicQueryParameters
            else
                operationObject.parameters += ParameterObjectConverter.toParameter(locationClass)
        }

        if (requestBodyType != typeOf<Unit>()) {
            operationObject.requestBody = RequestBodyObjectConverter.toRequestBody(components, requestBodyType)
        }

        if (locationClass == DynamicQueryLocation::class)
            operationObject.addSuccessResponse(BuiltinComponents.buildDynamicQueryResponse(components, responseBodyType))
        else
            operationObject.addSuccessResponse(ResponseObjectConverter.toResponse(components, responseBodyType))
    }

    private fun routeAuth(route: Route): List<PrincipalAuth>? {
        var current: Route? = route
        while (current != null) {
            if (current.attributes.contains(AuthorizedRoutePrincipalAuthsKey)) {
                return current.attributes[AuthorizedRoutePrincipalAuthsKey]
            }
            current = current.parent
        }
        return null
    }

    private fun routePath(route: Route, path: String?): String {
        var openApiPath = routeSelectorPath(route.selector) + (path ?: "")
        var current = route
        while (current.parent?.parent?.parent != null) {
            val parent = current.parent!!
            if (parent.selector !is AuthenticationRouteSelector) {
                openApiPath = routeSelectorPath(parent.selector) + openApiPath
            }
            current = parent
        }
        return openApiPath
    }

    private fun routeSelectorPath(selector: RouteSelector): String {
        return when (selector) {
            is PathSegmentConstantRouteSelector, is PathSegmentParameterRouteSelector -> "/$selector"
            else -> ""
        }
    }
}