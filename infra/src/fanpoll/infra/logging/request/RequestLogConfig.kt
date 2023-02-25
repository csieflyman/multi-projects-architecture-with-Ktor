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

    class Builder {
        var enabled: Boolean = true
        var destination: LogDestination = LogDestination.File
        var includeHeaders: Boolean = false
        var includeQuerystring: Boolean = false
        var includeResponseBody: Boolean = false
        var includeGetMethod: Boolean = false
        var excludePaths: MutableList<String> = mutableListOf()
        var excludeRequestBodyPaths: MutableList<String> = mutableListOf()

        fun build(): RequestLogConfig = RequestLogConfig(
            enabled, destination,
            includeHeaders, includeQuerystring, includeResponseBody,
            includeGetMethod,
            excludePaths, excludeRequestBodyPaths
        )
    }

    fun isExcludePath(call: ApplicationCall) = excludePaths.any { call.request.path().startsWith(it) }
}