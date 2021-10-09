/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.login.logging

import fanpoll.infra.logging.LogDestination

data class LoginLogConfig(
    val enabled: Boolean = true,
    val destination: LogDestination = LogDestination.File,
) {

    class Builder {
        var enabled: Boolean = true
        var destination: LogDestination = LogDestination.File

        fun build(): LoginLogConfig {
            return LoginLogConfig(enabled, destination)
        }
    }
}