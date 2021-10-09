/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.login.session

import fanpoll.infra.base.config.ValidateableConfig
import java.time.Duration

data class SessionConfig(
    val expireDuration: Duration? = null,
    val extendDuration: Duration? = null
) : ValidateableConfig {

    override fun validate() {
        require(if (expireDuration != null && extendDuration != null) expireDuration > extendDuration else true) {
            "expireDuration $expireDuration should be larger than extendDuration $extendDuration"
        }
    }

    class Builder {
        var expireDuration: Duration? = null
        var extendDuration: Duration? = null

        fun build(): SessionConfig {
            return SessionConfig(expireDuration, extendDuration).apply { validate() }
        }
    }
}