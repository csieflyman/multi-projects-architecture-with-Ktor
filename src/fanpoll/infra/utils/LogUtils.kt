/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.utils

import fanpoll.infra.auth.ServiceAuth.ATTRIBUTE_KEY_SOURCE
import fanpoll.infra.controller.toMyString
import io.ktor.application.ApplicationCall
import io.ktor.auth.Principal
import io.ktor.auth.principal
import io.ktor.request.ApplicationRequest

val ApplicationCall.sourceLogString: String
    get() = attributes.getOrNull(ATTRIBUTE_KEY_SOURCE)?.id ?: "Unknown-Source"

val ApplicationCall.principalLogString: String
    get() = principal<Principal>()?.toString() ?: "Unknown-Principal"

val ApplicationCall.subjectLogString: String
    get() = principal<Principal>()?.toString() ?: attributes.getOrNull(ATTRIBUTE_KEY_SOURCE)?.id ?: "Unknown-Subject"

val ApplicationRequest.toMyLogString: String
    get() = "${call.subjectLogString} - $toMyString"

fun ApplicationCall?.callerLogString(): String {
    return this?.request?.toMyLogString ?: "InternalCall"
}
