/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.auth

import fanpoll.infra.auth.login.logging.LoginLogConfig
import fanpoll.infra.config.ValidateableConfig
import fanpoll.infra.session.SessionStorageType

data class SessionAuthConfig(
    val storageType: SessionStorageType,
    val logging: LoginLogConfig,
    val redisKeyExpiredNotification: Boolean? = null
) : ValidateableConfig {
    override fun validate() {
        if (redisKeyExpiredNotification == true)
            require(storageType == SessionStorageType.Redis) {
                "storageType should be Redis if redisKeyExpiredNotification = true"
            }
    }
}