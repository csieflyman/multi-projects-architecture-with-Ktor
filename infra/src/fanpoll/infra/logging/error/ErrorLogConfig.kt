/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.error

import fanpoll.infra.logging.LogDestination

data class ErrorLogConfig(
    val enabled: Boolean = true,
    val destination: LogDestination = LogDestination.File,
    val sentry: SentryConfig? = null
) {

    class Builder {
        var enabled: Boolean = true
        var destination: LogDestination = LogDestination.File

        private var sentry: SentryConfig? = null

        fun sentry(block: SentryConfig.Builder.() -> Unit) {
            sentry = SentryConfig.Builder().apply(block).build()
        }

        fun build(): ErrorLogConfig {
            return ErrorLogConfig(enabled, destination, sentry)
        }
    }
}