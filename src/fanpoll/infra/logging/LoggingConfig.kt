/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.logging

import fanpoll.infra.utils.ConfigUtils
import fanpoll.infra.utils.CoroutineConfig
import fanpoll.infra.utils.MyConfig
import io.ktor.application.ApplicationCall
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import java.time.Duration

enum class StorageType {
    DB, Kinesis
}

data class LoggingConfig(
    val storage: StorageType,
    val kinesis: KinesisConfig?,
    val requestLogEnabled: Boolean = false,
    val errorLogEnabled: Boolean = true,
    val requestLog: RequestLogConfig,
    val coroutine: CoroutineConfig
) : MyConfig {

    var requestBodySensitiveDataFilter: ((call: ApplicationCall) -> String?)? = null
    var responseBodySensitiveDataFilter: ((call: ApplicationCall, body: String) -> String?)? = null

    override fun validate() {
        if (storage == StorageType.Kinesis) {
            ConfigUtils.require(kinesis != null) {
                "log.kinesis should be configured if log.storage == Kinesis"
            }
        }
    }
}

data class RequestLogConfig(
    val includeResponseBody: Boolean = false,
    val includeGetMethod: Boolean = false,
    val excludePaths: MutableList<String> = mutableListOf()
) {
    companion object {
        val includeHttpMethods = setOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Patch, HttpMethod.Delete)
        val includeBodyHttpMethods = setOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Patch)
        val includeContentTypes = setOf(ContentType.Application.Json, ContentType.Text.Plain)
    }
}

data class KinesisConfig(
    val commonStreamName: String,
    val errorStreamName: String,
    val asyncHttpClient: AwsAsyncHttpClientConfig
)

data class AwsAsyncHttpClientConfig(
    val maxConcurrency: Int,
    val maxPendingConnectionAcquires: Int?,
    val maxIdleConnectionTimeout: Duration?
)