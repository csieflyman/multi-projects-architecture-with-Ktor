/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.error

import fanpoll.infra.logging.LogDestination

data class ErrorLogConfig(
    val enabled: Boolean = true,
    val destination: LogDestination = LogDestination.File
)