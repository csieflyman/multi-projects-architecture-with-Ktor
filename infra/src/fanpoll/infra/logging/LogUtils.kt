/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.logging

import fanpoll.infra.auth.principal.PrincipalSource
import io.ktor.application.ApplicationCall
import io.ktor.auth.Principal
import io.ktor.auth.principal
import io.ktor.features.callId
import io.ktor.http.HttpHeaders
import io.ktor.request.*
import io.ktor.util.AttributeKey
import java.time.Instant
import java.util.*

object RequestAttribute {

    val TRACE_ID = AttributeKey<UUID>("X-Trace-ID")
    val REQ_ID = AttributeKey<UUID>(HttpHeaders.XRequestId)
    val PARENT_REQ_ID = AttributeKey<UUID>("X-Parent-Request-ID")

    val REQ_AT = AttributeKey<Instant>("reqAt")

    val TAGS = AttributeKey<Map<String, String>>("tags")

    val CLIENT_REQ_ID = AttributeKey<String>("clientReqId")
}

fun ApplicationCall?.logCaller(): String {
    return this?.let {
        "($callId) - " +
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