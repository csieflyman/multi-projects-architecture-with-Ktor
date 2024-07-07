/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.async

import fanpoll.infra.config.ValidateableConfig

data class ThreadPoolConfig(
    val fixedPoolSize: Int? = 1,
    val minPoolSize: Int? = null,
    val maxPoolSize: Int? = null,
    val keepAliveTime: Long? = null
) : ValidateableConfig {

    fun isFixedThreadPool(): Boolean = fixedPoolSize != null && (minPoolSize == null && maxPoolSize == null && keepAliveTime == null)

    override fun validate() {
        require(
            (minPoolSize != null && maxPoolSize != null && keepAliveTime != null) ||
                    (minPoolSize == null && maxPoolSize == null && keepAliveTime == null)
        ) {
            "minPoolSize, maxPoolSize, keepAliveTime should be configured"
        }
    }
}