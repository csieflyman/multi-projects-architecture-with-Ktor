/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.session

import fanpoll.infra.config.ValidateableConfig
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
}