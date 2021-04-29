/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */
@file:OptIn(KtorExperimentalLocationsAPI::class)

package fanpoll.infra.openapi

import fanpoll.infra.auth.AuthorizationRouteSelector
import fanpoll.infra.auth.PrincipalAuth
import fanpoll.infra.controller.EntityDTO
import fanpoll.infra.controller.Form
import fanpoll.infra.controller.MyLocation
import fanpoll.infra.controller.receiveAndValidate
import fanpoll.infra.database.DynamicDBQuery
import fanpoll.infra.openapi.KtorRouteHelper.routeAuth
import fanpoll.infra.openapi.KtorRouteHelper.routePath
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

@ContextDsl
inline fun <reified REQUEST : Form<*>, reified RESPONSE : Any> Route.post(
    operation: OpenApiOperation,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(REQUEST) -> Unit
): Route {
    operation.bindRoute(
        routeAuth(this), routePath(this), HttpMethod.Post,
        typeOf<REQUEST>(), typeOf<RESPONSE>()
    )
    return post { body(this, call.receiveAndValidate()) }
}

@ContextDsl
inline fun <reified REQUEST : Form<*>, reified RESPONSE : Any> Route.post(
    path: String,
    operation: OpenApiOperation,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(REQUEST) -> Unit
): Route {
    operation.bindRoute(
        routeAuth(this), routePath(this, path), HttpMethod.Post,
        typeOf<REQUEST>(), typeOf<RESPONSE>()
    )
    return post(path) { body(this, call.receiveAndValidate()) }
}

@ContextDsl
inline fun <reified REQUEST : Form<*>, reified RESPONSE : Any> Route.put(
    operation: OpenApiOperation,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(REQUEST) -> Unit
): Route {
    operation.bindRoute(
        routeAuth(this), routePath(this), HttpMethod.Put,
        typeOf<REQUEST>(), typeOf<RESPONSE>()
    )
    return put { body(this, call.receiveAndValidate()) }
}

@ContextDsl
inline fun <reified REQUEST : Form<*>, reified RESPONSE : Any> Route.put(
    path: String,
    operation: OpenApiOperation,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(REQUEST) -> Unit
): Route {
    operation.bindRoute(
        routeAuth(this), routePath(this, path), HttpMethod.Put,
        typeOf<REQUEST>(), typeOf<RESPONSE>()
    )
    return put(path) { body(this, call.receiveAndValidate()) }
}

@ContextDsl
inline fun <reified REQUEST : Form<*>, reified RESPONSE : Any> Route.patch(
    operation: OpenApiOperation,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(REQUEST) -> Unit
): Route {
    operation.bindRoute(
        routeAuth(this), routePath(this), HttpMethod.Patch,
        typeOf<REQUEST>(), typeOf<RESPONSE>()
    )
    return patch { body(this, call.receiveAndValidate()) }
}

@ContextDsl
inline fun <reified REQUEST : Form<*>, reified RESPONSE : Any> Route.patch(
    path: String,
    operation: OpenApiOperation,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(REQUEST) -> Unit
): Route {
    operation.bindRoute(
        routeAuth(this), routePath(this, path), HttpMethod.Patch,
        typeOf<REQUEST>(), typeOf<RESPONSE>()
    )
    return patch(path) { body(this, call.receiveAndValidate()) }
}

@ContextDsl
inline fun <reified RESPONSE : Any> Route.get(
    operation: OpenApiOperation,
    noinline body: PipelineInterceptor<Unit, ApplicationCall>
): Route {
    operation.bindRoute(
        routeAuth(this), routePath(this), HttpMethod.Get,
        typeOf<Unit>(), typeOf<RESPONSE>()
    )
    return get(body)
}

@ContextDsl
inline fun <reified RESPONSE : Any> Route.get(
    path: String,
    operation: OpenApiOperation,
    noinline body: PipelineInterceptor<Unit, ApplicationCall>
): Route {
    operation.bindRoute(
        routeAuth(this), routePath(this, path), HttpMethod.Get,
        typeOf<Unit>(), typeOf<RESPONSE>()
    )
    return get(path, body)
}

@ContextDsl
fun Route.delete(operation: OpenApiOperation, body: PipelineInterceptor<Unit, ApplicationCall>): Route {
    operation.bindRoute(
        routeAuth(this), routePath(this), HttpMethod.Delete,
        typeOf<Unit>(), typeOf<Unit>()
    )
    return delete(body)
}

@ContextDsl
fun Route.delete(path: String, operation: OpenApiOperation, body: PipelineInterceptor<Unit, ApplicationCall>): Route {
    operation.bindRoute(
        routeAuth(this), routePath(this, path), HttpMethod.Delete,
        typeOf<Unit>(), typeOf<Unit>()
    )
    return delete(path, body)
}

@ContextDsl
fun Route.postEmptyBody(
    operation: OpenApiOperation,
    body: PipelineInterceptor<Unit, ApplicationCall>
): Route {
    operation.bindRoute(
        routeAuth(this), routePath(this), HttpMethod.Post,
        typeOf<Unit>(), typeOf<Unit>()
    )
    return post(body)
}

@ContextDsl
fun Route.postEmptyBody(
    path: String,
    operation: OpenApiOperation,
    body: PipelineInterceptor<Unit, ApplicationCall>
): Route {
    operation.bindRoute(
        routeAuth(this), routePath(this, path), HttpMethod.Post,
        typeOf<Unit>(), typeOf<Unit>()
    )
    return post(path, body)
}

@ContextDsl
inline fun <reified RESPONSE : Any> Route.getList(
    operation: OpenApiOperation,
    noinline body: PipelineInterceptor<Unit, ApplicationCall>
): Route {
    operation.bindRoute(
        routeAuth(this), routePath(this), HttpMethod.Get,
        typeOf<Unit>(), typeOf<List<RESPONSE>>()
    )
    return get(body)
}

@ContextDsl
inline fun <reified RESPONSE : Any> Route.getList(
    path: String,
    operation: OpenApiOperation,
    noinline body: PipelineInterceptor<Unit, ApplicationCall>
): Route {
    operation.bindRoute(
        routeAuth(this), routePath(this, path), HttpMethod.Get,
        typeOf<Unit>(), typeOf<List<RESPONSE>>()
    )
    return get(path, body)
}

@ContextDsl
inline fun <reified LOCATION : Any, reified REQUEST : Form<*>, reified RESPONSE : Any> Route.post(
    operation: OpenApiOperation,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(LOCATION, REQUEST) -> Unit
): Route {
    operation.bindRoute(
        routeAuth(this), routePath(this), HttpMethod.Post,
        typeOf<REQUEST>(), typeOf<RESPONSE>(), LOCATION::class
    )
    return post<LOCATION> {
        body(this, it, call.receiveAndValidate(it as? MyLocation))
    }
}

@ContextDsl
inline fun <reified LOCATION : Any, reified REQUEST : Form<*>, reified RESPONSE : Any> Route.put(
    operation: OpenApiOperation,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(LOCATION, REQUEST) -> Unit
): Route {
    operation.bindRoute(
        routeAuth(this), routePath(this), HttpMethod.Put,
        typeOf<REQUEST>(), typeOf<RESPONSE>(), LOCATION::class
    )
    return put<LOCATION> {
        body(this, it, call.receiveAndValidate(it as? MyLocation))
    }
}

@ContextDsl
inline fun <reified LOCATION : Any, reified REQUEST : Form<*>, reified RESPONSE : Any> Route.patch(
    operation: OpenApiOperation,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(LOCATION, REQUEST) -> Unit
): Route {
    operation.bindRoute(
        routeAuth(this), routePath(this), HttpMethod.Patch,
        typeOf<REQUEST>(), typeOf<RESPONSE>(), LOCATION::class
    )
    return patch<LOCATION> {
        body(this, it, call.receiveAndValidate(it as? MyLocation))
    }
}

@ContextDsl
inline fun <reified LOCATION : Any, reified RESPONSE : Any> Route.getWithLocation(
    operation: OpenApiOperation,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(LOCATION) -> Unit
): Route {
    operation.bindRoute(
        routeAuth(this), routePath(this), HttpMethod.Get,
        typeOf<Unit>(), typeOf<RESPONSE>(), LOCATION::class
    )
    return get(body)
}

@ContextDsl
inline fun <reified LOCATION : Any> Route.deleteWithLocation(
    operation: OpenApiOperation,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(LOCATION) -> Unit
): Route {
    operation.bindRoute(
        routeAuth(this), routePath(this), HttpMethod.Delete,
        typeOf<Unit>(), typeOf<Unit>(), LOCATION::class
    )
    return delete(body)
}

@ContextDsl
inline fun <reified RESPONSE : EntityDTO<*>> Route.dynamicQuery(
    operation: OpenApiOperation,
    noinline body: PipelineInterceptor<Unit, ApplicationCall>
): Route {
    operation.bindRoute(
        routeAuth(this), routePath(this), HttpMethod.Get,
        typeOf<Unit>(), typeOf<DynamicDBQuery<RESPONSE>>()
    )
    return get(body)
}

@ContextDsl
inline fun <reified RESPONSE : EntityDTO<*>> Route.dynamicQuery(
    path: String,
    operation: OpenApiOperation,
    noinline body: PipelineInterceptor<Unit, ApplicationCall>
): Route {
    operation.bindRoute(
        routeAuth(this), routePath(this, path), HttpMethod.Get,
        typeOf<Unit>(), typeOf<DynamicDBQuery<RESPONSE>>()
    )
    return get(path, body)
}

object KtorRouteHelper {

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
}
