/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */
@file:OptIn(KtorExperimentalLocationsAPI::class)

package fanpoll.infra.openapi.support

import fanpoll.infra.auth.AuthorizationRouteSelector
import fanpoll.infra.auth.PrincipalAuth
import fanpoll.infra.controller.EntityDTO
import fanpoll.infra.controller.Form
import fanpoll.infra.controller.MyLocation
import fanpoll.infra.controller.receiveAndValidate
import fanpoll.infra.database.DynamicDBQuery
import fanpoll.infra.openapi.OpenApiManager
import fanpoll.infra.openapi.support.OpenApiRouteSupport.UnitKType
import fanpoll.infra.openapi.support.OpenApiRouteSupport.routeAuth
import fanpoll.infra.openapi.support.OpenApiRouteSupport.routePath
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.AuthenticationRouteSelector
import io.ktor.http.HttpMethod
import io.ktor.locations.*
import io.ktor.routing.*
import io.ktor.util.pipeline.ContextDsl
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.pipeline.PipelineInterceptor
import kotlin.reflect.typeOf

//==================== Route by Location ====================

@ContextDsl
inline fun <reified LOCATION : Any, reified REQUEST : Form<*>, reified RESPONSE : Any> Route.postLocation(
    operation: OpenApiRoute,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(LOCATION, REQUEST) -> Unit
): Route {
    OpenApiManager.bindRoute(
        routeAuth(this), routePath(this),
        HttpMethod.Post, operation,
        typeOf<REQUEST>(), typeOf<RESPONSE>(), LOCATION::class
    )
    return post<LOCATION> {
        body(this, it, call.receiveAndValidate(it as? MyLocation))
    }
}

@ContextDsl
inline fun <reified LOCATION : Any, reified REQUEST : Form<*>, reified RESPONSE : Any> Route.putLocation(
    operation: OpenApiRoute,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(LOCATION, REQUEST) -> Unit
): Route {
    OpenApiManager.bindRoute(
        routeAuth(this), routePath(this),
        HttpMethod.Put, operation,
        typeOf<REQUEST>(), typeOf<RESPONSE>(), LOCATION::class
    )
    return put<LOCATION> {
        body(this, it, call.receiveAndValidate(it as? MyLocation))
    }
}

@ContextDsl
inline fun <reified LOCATION : Any, reified REQUEST : Form<*>, reified RESPONSE : Any> Route.patchLocation(
    operation: OpenApiRoute,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(LOCATION, REQUEST) -> Unit
): Route {
    OpenApiManager.bindRoute(
        routeAuth(this), routePath(this),
        HttpMethod.Patch, operation,
        typeOf<REQUEST>(), typeOf<RESPONSE>(), LOCATION::class
    )
    return patch<LOCATION> {
        body(this, it, call.receiveAndValidate(it as? MyLocation))
    }
}

@ContextDsl
inline fun <reified LOCATION : Any, reified RESPONSE : Any> Route.getLocation(
    operation: OpenApiRoute,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(LOCATION) -> Unit
): Route {
    OpenApiManager.bindRoute(
        routeAuth(this), routePath(this),
        HttpMethod.Get, operation,
        UnitKType, typeOf<RESPONSE>(), LOCATION::class
    )
    return get(body)
}

@ContextDsl
inline fun <reified LOCATION : Any> Route.deleteLocation(
    operation: OpenApiRoute,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(LOCATION) -> Unit
): Route {
    OpenApiManager.bindRoute(routeAuth(this), routePath(this), HttpMethod.Delete, operation, UnitKType, UnitKType, LOCATION::class)
    return delete(body)
}

//==================== Route by Path ====================

@ContextDsl
fun Route.postEmptyBody(
    operation: OpenApiRoute,
    body: PipelineInterceptor<Unit, ApplicationCall>
): Route {
    OpenApiManager.bindRoute(routeAuth(this), routePath(this), HttpMethod.Post, operation, UnitKType, UnitKType)
    return post(body)
}

@ContextDsl
fun Route.postEmptyBody(
    path: String,
    operation: OpenApiRoute,
    body: PipelineInterceptor<Unit, ApplicationCall>
): Route {
    OpenApiManager.bindRoute(routeAuth(this), routePath(this, path), HttpMethod.Post, operation, UnitKType, UnitKType)
    return post(path, body)
}

@ContextDsl
inline fun <reified REQUEST : Form<*>, reified RESPONSE : Any> Route.post(
    operation: OpenApiRoute,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(REQUEST) -> Unit
): Route {
    OpenApiManager.bindRoute(routeAuth(this), routePath(this), HttpMethod.Post, operation, typeOf<REQUEST>(), typeOf<RESPONSE>())
    return post { body(this, call.receiveAndValidate()) }
}

@ContextDsl
inline fun <reified REQUEST : Form<*>, reified RESPONSE : Any> Route.post(
    path: String,
    operation: OpenApiRoute,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(REQUEST) -> Unit
): Route {
    OpenApiManager.bindRoute(
        routeAuth(this), routePath(this, path),
        HttpMethod.Post, operation,
        typeOf<REQUEST>(), typeOf<RESPONSE>()
    )
    return post(path) { body(this, call.receiveAndValidate()) }
}

@ContextDsl
inline fun <reified REQUEST : Form<*>, reified RESPONSE : Any> Route.put(
    operation: OpenApiRoute,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(REQUEST) -> Unit
): Route {
    OpenApiManager.bindRoute(routeAuth(this), routePath(this), HttpMethod.Put, operation, typeOf<REQUEST>(), typeOf<RESPONSE>())
    return put { body(this, call.receiveAndValidate()) }
}

@ContextDsl
inline fun <reified REQUEST : Form<*>, reified RESPONSE : Any> Route.put(
    path: String,
    operation: OpenApiRoute,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(REQUEST) -> Unit
): Route {
    OpenApiManager.bindRoute(
        routeAuth(this), routePath(this, path),
        HttpMethod.Put, operation,
        typeOf<REQUEST>(), typeOf<RESPONSE>()
    )
    return put(path) { body(this, call.receiveAndValidate()) }
}

@ContextDsl
inline fun <reified REQUEST : Form<*>, reified RESPONSE : Any> Route.patch(
    operation: OpenApiRoute,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(REQUEST) -> Unit
): Route {
    OpenApiManager.bindRoute(routeAuth(this), routePath(this), HttpMethod.Patch, operation, typeOf<REQUEST>(), typeOf<RESPONSE>())
    return patch { body(this, call.receiveAndValidate()) }
}

@ContextDsl
inline fun <reified REQUEST : Form<*>, reified RESPONSE : Any> Route.patch(
    path: String,
    operation: OpenApiRoute,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(REQUEST) -> Unit
): Route {
    OpenApiManager.bindRoute(
        routeAuth(this), routePath(this, path),
        HttpMethod.Patch, operation,
        typeOf<REQUEST>(), typeOf<RESPONSE>()
    )
    return patch(path) { body(this, call.receiveAndValidate()) }
}

@ContextDsl
inline fun <reified RESPONSE : Any> Route.get(
    operation: OpenApiRoute,
    noinline body: PipelineInterceptor<Unit, ApplicationCall>
): Route {
    OpenApiManager.bindRoute(routeAuth(this), routePath(this), HttpMethod.Get, operation, UnitKType, typeOf<RESPONSE>())
    return get(body)
}

@ContextDsl
inline fun <reified RESPONSE : Any> Route.get(
    path: String,
    operation: OpenApiRoute,
    noinline body: PipelineInterceptor<Unit, ApplicationCall>
): Route {
    OpenApiManager.bindRoute(routeAuth(this), routePath(this, path), HttpMethod.Get, operation, UnitKType, typeOf<RESPONSE>())
    return get(path, body)
}

@ContextDsl
inline fun <reified RESPONSE : Any> Route.getList(
    operation: OpenApiRoute,
    noinline body: PipelineInterceptor<Unit, ApplicationCall>
): Route {
    OpenApiManager.bindRoute(
        routeAuth(this), routePath(this),
        HttpMethod.Get, operation,
        UnitKType, typeOf<List<RESPONSE>>()
    )
    return get(body)
}

@ContextDsl
inline fun <reified RESPONSE : Any> Route.getList(
    path: String,
    operation: OpenApiRoute,
    noinline body: PipelineInterceptor<Unit, ApplicationCall>
): Route {
    OpenApiManager.bindRoute(
        routeAuth(this), routePath(this, path),
        HttpMethod.Get, operation,
        UnitKType, typeOf<List<RESPONSE>>()
    )
    return get(path, body)
}

@ContextDsl
inline fun <reified RESPONSE : EntityDTO<*>> Route.dynamicQuery(
    operation: OpenApiRoute,
    noinline body: PipelineInterceptor<Unit, ApplicationCall>
): Route {
    OpenApiManager.bindRoute(
        routeAuth(this), routePath(this),
        HttpMethod.Get, operation,
        UnitKType, typeOf<DynamicDBQuery<RESPONSE>>()
    )
    return get(body)
}

@ContextDsl
inline fun <reified RESPONSE : EntityDTO<*>> Route.dynamicQuery(
    path: String,
    operation: OpenApiRoute,
    noinline body: PipelineInterceptor<Unit, ApplicationCall>
): Route {
    OpenApiManager.bindRoute(
        routeAuth(this), routePath(this, path),
        HttpMethod.Get, operation,
        UnitKType, typeOf<DynamicDBQuery<RESPONSE>>()
    )
    return get(path, body)
}

@ContextDsl
fun Route.delete(operation: OpenApiRoute, body: PipelineInterceptor<Unit, ApplicationCall>): Route {
    OpenApiManager.bindRoute(routeAuth(this), routePath(this), HttpMethod.Delete, operation, UnitKType, UnitKType)
    return delete(body)
}

@ContextDsl
fun Route.delete(path: String, operation: OpenApiRoute, body: PipelineInterceptor<Unit, ApplicationCall>): Route {
    OpenApiManager.bindRoute(routeAuth(this), routePath(this, path), HttpMethod.Delete, operation, UnitKType, UnitKType)
    return delete(path, body)
}

object OpenApiRouteSupport {

    val routeType = typeOf<OpenApiRoute>()

    val UnitKType = typeOf<Unit>()

    fun routePath(route: Route, path: String = ""): String {
        var openApiPath = routeSelectorPath(route.selector) + path
        var current = route
        while (current.parent?.parent?.parent != null) {
            val parent = current.parent!!
            if (parent.selector !is AuthenticationRouteSelector || parent.selector !is AuthorizationRouteSelector) {
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

    fun routeAuth(route: Route): List<PrincipalAuth>? {
        var current: Route? = route
        while (current != null) {
            if (current.selector is AuthorizationRouteSelector) {
                return (current.selector as AuthorizationRouteSelector).principalAuths
            }
            current = current.parent
        }
        return null
    }

}
