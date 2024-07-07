/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.base.httpclient

import io.ktor.client.plugins.logging.LogLevel

// https://ktor.io/docs/http-client-engines.html#jvm

interface HttpClientConfig {

    val threadsCount: Int
    val logLevel: LogLevel
}

// ASSUMPTION => only use CIO Engine now
data class CIOHttpClientConfig(
    override val threadsCount: Int = 1,
    override val logLevel: LogLevel = LogLevel.NONE,
    val maxConnectionsCount: Int = 1000,
    val requestTimeout: Long = 15000,
    val connectTimeout: Long = 5000,
    val connectAttempts: Int = 1,
    val keepAliveTime: Long = 5000
) : HttpClientConfig