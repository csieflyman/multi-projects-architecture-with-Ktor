/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.database.jasync

data class JasyncConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val maxActiveConnections: Int,
    val maxIdleTime: Long,
    val connectionCreateTimeout: Long,
    val connectionTestTimeout: Long,
    val queryTimeout: Long? = null
)