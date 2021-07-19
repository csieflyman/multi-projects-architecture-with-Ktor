/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.aws

import fanpoll.infra.base.async.ThreadPoolConfig
import fanpoll.infra.base.async.ThreadPoolUtils
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder
import software.amazon.awssdk.core.client.config.ClientAsyncConfiguration
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import java.time.Duration

data class NettyHttpClientConfig(
    val http: NettyNioAsyncHttpClientConfig? = null,
    val threadPool: ThreadPoolConfig? = null
) {

    class Builder {

        private var httpConfig: NettyNioAsyncHttpClientConfig? = null
        private var threadPoolConfig: ThreadPoolConfig? = null

        fun http(block: NettyNioAsyncHttpClientConfig.Builder.() -> Unit): NettyNioAsyncHttpClientConfig {
            return NettyNioAsyncHttpClientConfig.Builder().apply(block).build()
        }

        fun threadPool(block: ThreadPoolConfig.Builder.() -> Unit) {
            threadPoolConfig = ThreadPoolConfig.Builder().apply(block).build()
        }

        fun build(): NettyHttpClientConfig {
            return NettyHttpClientConfig(httpConfig, threadPoolConfig)
        }
    }
}

data class NettyNioAsyncHttpClientConfig(
    val maxConcurrency: Int? = null,
    val maxPendingConnectionAcquires: Int? = null,
    val maxIdleConnectionTimeout: Duration? = null
) {
    class Builder {

        var maxConcurrency: Int? = null
        var maxPendingConnectionAcquires: Int? = null
        var maxIdleConnectionTimeout: Duration? = null

        fun build(): NettyNioAsyncHttpClientConfig {
            return NettyNioAsyncHttpClientConfig(maxConcurrency, maxPendingConnectionAcquires, maxIdleConnectionTimeout)
        }
    }
}

fun AwsAsyncClientBuilder<*, *>.configure(name: String, config: NettyHttpClientConfig) {
    httpClientBuilder(NettyNioAsyncHttpClient.builder().configureHttp(config.http))
    if (config.threadPool != null) {
        asyncConfiguration(
            ClientAsyncConfiguration.builder().advancedOption(
                SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR,
                ThreadPoolUtils.createThreadPoolExecutor(name, config.threadPool)
            ).build()
        )
    }
}

fun NettyNioAsyncHttpClient.Builder.configureHttp(config: NettyNioAsyncHttpClientConfig?): NettyNioAsyncHttpClient.Builder {
    if (config != null) {
        if (config.maxConcurrency != null)
            maxConcurrency(config.maxConcurrency)
        if (config.maxPendingConnectionAcquires != null)
            maxPendingConnectionAcquires(config.maxPendingConnectionAcquires)
        if (config.maxIdleConnectionTimeout != null)
            connectionMaxIdleTime(config.maxIdleConnectionTimeout)
    }
    return this
}