/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.request

import fanpoll.infra.logging.LogDestination
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.path

data class RequestLogConfig(
    val enabled: Boolean = true,
    var destination: LogDestination = LogDestination.File,
    val includeHeaders: Boolean = false,
    val includeQueryString: Boolean = false,
    val includeResponseBody: Boolean = false,
    val includeGetMethod: Boolean = false,
    val excludePaths: MutableList<String> = mutableListOf(),
    val excludeRequestBodyPaths: MutableList<String> = mutableListOf()
) {
    fun isExcludePath(call: ApplicationCall) = excludePaths.any { call.request.path().startsWith(it) }
}