/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.request

import fanpoll.infra.logging.LogDestination

data class RequestLogConfig(
    val enabled: Boolean = true,
    var destination: LogDestination = LogDestination.File,
    val includeHeaders: Boolean = false,
    val includeQueryString: Boolean = false,
    val includeResponseBody: Boolean = false,
    val includeGetMethod: Boolean = false,
    val excludePaths: MutableList<String> = mutableListOf(),
    val excludeRequestBodyPaths: MutableList<String> = mutableListOf(),
    val loki: LokiLogConfig? = null
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

        private var loki: LokiLogConfig? = null

        fun loki(block: LokiLogConfig.Builder.() -> Unit) {
            loki = LokiLogConfig.Builder().apply(block).build()
        }

        fun build(): RequestLogConfig = RequestLogConfig(
            enabled, destination,
            includeHeaders, includeQuerystring, includeResponseBody,
            includeGetMethod,
            excludePaths, excludeRequestBodyPaths,
            loki
        )
    }
}