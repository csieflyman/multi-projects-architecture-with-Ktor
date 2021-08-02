/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.config

import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode

interface ValidateableConfig {

    fun validate()

    fun require(value: Boolean, lazyMessage: () -> Any) {
        try {
            kotlin.require(value, lazyMessage)
        } catch (e: IllegalArgumentException) {
            throw InternalServerException(InfraResponseCode.SERVER_CONFIG_ERROR, e.message)
        }
    }
}