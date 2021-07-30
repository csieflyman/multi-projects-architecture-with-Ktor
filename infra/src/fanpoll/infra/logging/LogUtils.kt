/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.logging

import fanpoll.infra.auth.principal.PrincipalSource
import io.ktor.application.ApplicationCall
import io.ktor.auth.Principal
import io.ktor.auth.principal
import io.ktor.features.callId
import io.ktor.request.ApplicationRequest
import io.ktor.request.httpMethod
import io.ktor.request.queryString
import io.ktor.request.uri

fun ApplicationCall?.toCallerLogString(): String {
    return this?.let {
        "($callId) - " +
                (attributes.getOrNull(PrincipalSource.ATTRIBUTE_KEY)?.id ?: "Unknown-Source") + " - " +
                (principal<Principal>()?.toString() ?: "Unknown-Principal")
    } ?: "Internal"
}

fun ApplicationCall?.toLogString(): String = this?.toCallerLogString() + this?.request?.toApiLogString()?.let { " - $it" }

fun ApplicationRequest.toApiLogString(): String = "${httpMethod.value} - $uri"

fun ApplicationRequest.toQueryStringLogString(): String? = queryString().let { it.ifEmpty { null } }

fun ApplicationRequest.toHeadersLogString(): String? = if (headers.isEmpty()) null
else headers.entries().joinToString(",") { "${it.key} = ${it.value}" }