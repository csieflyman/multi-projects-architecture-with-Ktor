/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.logging

import fanpoll.infra.auth.principal.PrincipalSource
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.Principal
import io.ktor.server.auth.principal
import io.ktor.server.request.*

fun ApplicationCall?.logCaller(): String {
    return this?.let {
        "(${attributes[RequestAttributeKey.ID]}) - " +
                (attributes.getOrNull(PrincipalSource.ATTRIBUTE_KEY)?.id ?: "Unknown-Source") + " - " +
                (principal<Principal>()?.toString() ?: "Unknown-Principal")
    } ?: "Internal"
}

fun ApplicationCall?.logString(): String = this?.logCaller() + this?.request?.logApi()?.let { " - $it" }

fun ApplicationRequest.logApi(): String = "${httpMethod.value} - $uri"

fun ApplicationRequest.logQueryString(): String? = queryString().let { it.ifEmpty { null } }

fun ApplicationRequest.logHeaders(): String? = if (headers.isEmpty()) null
else headers.entries().joinToString(",") { "${it.key} = ${it.value}" }

// ASSUMPTION: request path format => {host}/{project}/{function}
fun ApplicationRequest.logFunction(): String = path().split("/")[2]