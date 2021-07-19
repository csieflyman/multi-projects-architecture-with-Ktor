/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */
@file:OptIn(KtorExperimentalLocationsAPI::class)

package fanpoll.infra.openapi

import fanpoll.infra.base.entity.EntityDTO
import fanpoll.infra.base.form.Form
import fanpoll.infra.base.location.Location
import fanpoll.infra.base.location.receiveAndValidateBody
import fanpoll.infra.base.query.DynamicQuery
import fanpoll.infra.base.query.DynamicQueryLocation
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.patch
import io.ktor.routing.post
import io.ktor.routing.put
import io.ktor.util.pipeline.ContextDsl
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.pipeline.PipelineInterceptor
import kotlin.reflect.typeOf
import io.ktor.locations.delete as locationDelete
import io.ktor.locations.get as locationGet
import io.ktor.locations.patch as locationPatch
import io.ktor.locations.post as locationPost
import io.ktor.locations.put as locationPut

@ContextDsl
inline fun <reified REQUEST : Form<*>, reified RESPONSE : Any> Route.post(
    operation: OpenApiOperation,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(REQUEST) -> Unit
): Route {
    operation.bindRoute(
        this, null, HttpMethod.Post,
        typeOf<REQUEST>(), typeOf<RESPONSE>()
    )
    return post { body(this, call.receiveAndValidateBody()) }
}

@ContextDsl
inline fun <reified REQUEST : Form<*>, reified RESPONSE : Any> Route.post(
    path: String,
    operation: OpenApiOperation,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(REQUEST) -> Unit
): Route {
    operation.bindRoute(
        this, path, HttpMethod.Post,
        typeOf<REQUEST>(), typeOf<RESPONSE>()
    )
    return post(path) { body(this, call.receiveAndValidateBody()) }
}

@ContextDsl
inline fun <reified REQUEST : Form<*>, reified RESPONSE : Any> Route.put(
    operation: OpenApiOperation,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(REQUEST) -> Unit
): Route {
    operation.bindRoute(
        this, null, HttpMethod.Put,
        typeOf<REQUEST>(), typeOf<RESPONSE>()
    )
    return put { body(this, call.receiveAndValidateBody()) }
}

@ContextDsl
inline fun <reified REQUEST : Form<*>, reified RESPONSE : Any> Route.put(
    path: String,
    operation: OpenApiOperation,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(REQUEST) -> Unit
): Route {
    operation.bindRoute(
        this, path, HttpMethod.Put,
        typeOf<REQUEST>(), typeOf<RESPONSE>()
    )
    return put(path) { body(this, call.receiveAndValidateBody()) }
}

@ContextDsl
inline fun <reified REQUEST : Form<*>, reified RESPONSE : Any> Route.patch(
    operation: OpenApiOperation,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(REQUEST) -> Unit
): Route {
    operation.bindRoute(
        this, null, HttpMethod.Patch,
        typeOf<REQUEST>(), typeOf<RESPONSE>()
    )
    return patch { body(this, call.receiveAndValidateBody()) }
}

@ContextDsl
inline fun <reified REQUEST : Form<*>, reified RESPONSE : Any> Route.patch(
    path: String,
    operation: OpenApiOperation,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(REQUEST) -> Unit
): Route {
    operation.bindRoute(
        this, path, HttpMethod.Patch,
        typeOf<REQUEST>(), typeOf<RESPONSE>()
    )
    return patch(path) { body(this, call.receiveAndValidateBody()) }
}

@ContextDsl
inline fun <reified RESPONSE : Any> Route.get(
    operation: OpenApiOperation,
    noinline body: PipelineInterceptor<Unit, ApplicationCall>
): Route {
    operation.bindRoute(
        this, null, HttpMethod.Get,
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
        this, path, HttpMethod.Get,
        typeOf<Unit>(), typeOf<RESPONSE>()
    )
    return get(path, body)
}

@ContextDsl
fun Route.delete(operation: OpenApiOperation, body: PipelineInterceptor<Unit, ApplicationCall>): Route {
    operation.bindRoute(
        this, null, HttpMethod.Delete,
        typeOf<Unit>(), typeOf<Unit>()
    )
    return delete(body)
}

@ContextDsl
fun Route.delete(path: String, operation: OpenApiOperation, body: PipelineInterceptor<Unit, ApplicationCall>): Route {
    operation.bindRoute(
        this, path, HttpMethod.Delete,
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
        this, null, HttpMethod.Post,
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
        this, path, HttpMethod.Post,
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
        this, null, HttpMethod.Get,
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
        this, path, HttpMethod.Get,
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
        this, null, HttpMethod.Post,
        typeOf<REQUEST>(), typeOf<RESPONSE>(), LOCATION::class
    )
    return locationPost<LOCATION> {
        body(this, it, call.receiveAndValidateBody(it as? Location))
    }
}

@ContextDsl
inline fun <reified LOCATION : Location, reified REQUEST : Form<*>, reified RESPONSE : Any> Route.put(
    operation: OpenApiOperation,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(LOCATION, REQUEST) -> Unit
): Route {
    operation.bindRoute(
        this, null, HttpMethod.Put,
        typeOf<REQUEST>(), typeOf<RESPONSE>(), LOCATION::class
    )
    return locationPut<LOCATION> {
        body(this, it, call.receiveAndValidateBody(it as? Location))
    }
}

@ContextDsl
inline fun <reified LOCATION : Location, reified REQUEST : Form<*>, reified RESPONSE : Any> Route.patch(
    operation: OpenApiOperation,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(LOCATION, REQUEST) -> Unit
): Route {
    operation.bindRoute(
        this, null, HttpMethod.Patch,
        typeOf<REQUEST>(), typeOf<RESPONSE>(), LOCATION::class
    )
    return locationPatch<LOCATION> {
        body(this, it, call.receiveAndValidateBody(it as? Location))
    }
}

@OptIn(KtorExperimentalLocationsAPI::class)
@ContextDsl
inline fun <reified LOCATION : Location, reified RESPONSE : Any> Route.getWithLocation(
    operation: OpenApiOperation,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(LOCATION) -> Unit
): Route {
    operation.bindRoute(
        this, null, HttpMethod.Get,
        typeOf<Unit>(), typeOf<RESPONSE>(), LOCATION::class
    )
    return locationGet(body)
}

@ContextDsl
inline fun <reified LOCATION : Location> Route.deleteWithLocation(
    operation: OpenApiOperation,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(LOCATION) -> Unit
): Route {
    operation.bindRoute(
        this, null, HttpMethod.Delete,
        typeOf<Unit>(), typeOf<Unit>(), LOCATION::class
    )
    return locationDelete(body)
}

@ContextDsl
inline fun <reified RESPONSE : EntityDTO<*>> Route.dynamicQuery(
    operation: OpenApiOperation,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(DynamicQuery) -> Unit
): Route {
    operation.bindRoute(
        this, null, HttpMethod.Get,
        typeOf<Unit>(), typeOf<RESPONSE>(), DynamicQueryLocation::class
    )
    return locationGet<DynamicQueryLocation> {
        it.validate()
        body(this, DynamicQuery.from(it))
    }
}
