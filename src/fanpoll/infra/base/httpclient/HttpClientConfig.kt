/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.base.httpclient

import io.ktor.client.features.logging.LogLevel

// https://ktor.io/docs/http-client-engines.html#jvm

interface HttpClientConfig {

    val threadsCount: Int
    val logLevel: LogLevel
}

// ASSUMPTION => only use CIO Engine now
data class CIOHttpClientConfig(
    override val threadsCount: Int,
    override val logLevel: LogLevel = LogLevel.NONE,
    val maxConnectionsCount: Int,
    val requestTimeout: Long,
    val connectTimeout: Long,
    val connectRetryAttempts: Int?,
    val keepAliveTime: Long?
) : HttpClientConfig