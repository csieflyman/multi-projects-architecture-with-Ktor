/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.redis

import fanpoll.infra.MyApplicationConfig
import fanpoll.infra.base.async.CoroutineActorConfig
import fanpoll.infra.base.async.CoroutineUtils
import fanpoll.infra.base.async.ThreadPoolConfig
import fanpoll.infra.base.config.ValidateableConfig
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.koin.KoinApplicationShutdownManager
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.logging.writers.LogWriter
import fanpoll.infra.redis.ktorio.RedisClient
import fanpoll.infra.redis.ktorio.commands.ping
import fanpoll.infra.redis.ktorio.commands.psubscribe
import fanpoll.infra.redis.ktorio.commands.quit
import fanpoll.infra.redis.ktorio.commands.subscribe
import io.ktor.application.Application
import io.ktor.application.ApplicationFeature
import io.ktor.util.AttributeKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.ext.koin
import java.net.InetSocketAddress
import kotlin.system.measureTimeMillis

class RedisFeature(configuration: Configuration) {

    class Configuration {

        lateinit var host: String
        var port: Int = 6379
        var password: String? = null
        lateinit var rootKeyPrefix: String

        private lateinit var client: CoroutineActorConfig
        var subscribe: SubscribeConfig? = null

        fun client(block: CoroutineActorConfig.Builder.() -> Unit) {
            client = CoroutineActorConfig.Builder().apply(block).build()
        }

        fun subscribe(configure: SubscribeConfig.Builder.() -> Unit) {
            subscribe = SubscribeConfig.Builder().apply(configure).build()
        }

        fun build(): RedisConfig {
            return RedisConfig(host, port, password, rootKeyPrefix, client, subscribe)
        }
    }

    companion object Feature : ApplicationFeature<Application, Configuration, RedisFeature> {

        override val key = AttributeKey<RedisFeature>("Redis")

        private val logger = KotlinLogging.logger {}

        private lateinit var config: RedisConfig

        private lateinit var client: RedisClient
        private const val dispatcherName = "Redis"
        private lateinit var dispatcher: CoroutineDispatcher

        private lateinit var subscribeClient: RedisClient
        private const val subscribeDispatcherName = "Redis-Subscriber"
        private lateinit var subscribeDispatcher: CoroutineDispatcher

        private var keyspaceNotificationListener: RedisKeyspaceNotificationListener? = null

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): RedisFeature {
            val configuration = Configuration().apply(configure)
            val feature = RedisFeature(configuration)

            val appConfig = pipeline.get<MyApplicationConfig>()
            val logWriter = pipeline.get<LogWriter>()
            config = appConfig.infra.redis ?: configuration.build()

            initClient(config)

            if (config.subscribe != null) {
                initSubscriber(config, logWriter)
            }

            pipeline.koin {
                modules(
                    module(createdAtStart = true) {
                        single { client }
                        if (keyspaceNotificationListener != null)
                            single { keyspaceNotificationListener }
                        KoinApplicationShutdownManager.register { shutdown() }
                    }
                )
            }

            return feature
        }
        
        private fun initClient(config: RedisConfig) {
            logger.info("========== init Redis Client... ==========")
            try {
                dispatcher = if (config.client.dispatcher != null)
                    CoroutineUtils.createDispatcher(dispatcherName, config.client.dispatcher)
                else Dispatchers.IO

                logger.info("connect to redis => $config")
                client = RedisClient(
                    address = InetSocketAddress(config.host, config.port),
                    password = config.password,
                    maxConnections = config.client.coroutines,
                    dispatcher = dispatcher, rootKeyPrefix = config.rootKeyPrefix
                )

                runBlocking {
                    logger.info("ping...")
                    val latency = measureTimeMillis {
                        client.ping()?.let {
                            logger.info(it)
                        }
                    }
                    logger.info("ping latency = $latency milliseconds")
                }
            } catch (e: Throwable) {
                throw InternalServerException(InfraResponseCode.REDIS_ERROR, "fail to init redis client", e)
            }
            logger.info("========== init Redis Client completed ==========")
        }

        private fun initSubscriber(config: RedisConfig, logWriter: LogWriter) {
            logger.info("========== init Redis PubSub subscriber... ==========")
            try {
                subscribeDispatcher = CoroutineUtils.createDispatcher(subscribeDispatcherName, ThreadPoolConfig(fixedPoolSize = 1))
                subscribeClient = RedisClient(
                    address = InetSocketAddress(config.host, config.port),
                    password = config.password,
                    maxConnections = 1,
                    dispatcher = subscribeDispatcher, rootKeyPrefix = config.rootKeyPrefix
                )

                with(config.subscribe!!) {
                    val redisPubSub = runBlocking {
                        val subscriber = if (!patterns.isNullOrEmpty())
                            subscribeClient.psubscribe(*patterns.toTypedArray())
                        else null

                        if (!channels.isNullOrEmpty()) {
                            subscriber?.subscribe(*channels.toTypedArray()) ?: subscribeClient.subscribe(*channels.toTypedArray())
                        } else subscriber
                    }!!

                    if (keyspaceNotification != null) {
                        keyspaceNotificationListener = RedisKeyspaceNotificationListener(keyspaceNotification, redisPubSub, logWriter)
                    }
                }
            } catch (e: Throwable) {
                throw InternalServerException(InfraResponseCode.REDIS_ERROR, "fail to init Redis PubSub subscriber", e)
            }
            logger.info("========== init Redis PubSub subscriber completed ==========")
        }

        private fun shutdown() {
            closePubSubClient()
            closeClient()
        }

        private fun closePubSubClient() {
            if (config.subscribe != null) {
                try {
                    runBlocking {
                        logger.info("close Redis PubSub subscriber connection...")
                        subscribeClient.quit()
                        logger.info("Redis PubSub subscriber connection closed")
                    }

                    CoroutineUtils.closeDispatcher(subscribeDispatcherName, subscribeDispatcher as ExecutorCoroutineDispatcher)

                    keyspaceNotificationListener?.shutdown()
                } catch (e: Throwable) {
                    throw InternalServerException(InfraResponseCode.REDIS_ERROR, "fail to close Redis PubSub subscriber connection", e)
                }
            }
        }

        private fun closeClient() {
            try {
                runBlocking {
                    logger.info("close redis connection...")
                    client.quit()
                    logger.info("redis connection closed")
                }

                if (dispatcher is ExecutorCoroutineDispatcher) {
                    CoroutineUtils.closeDispatcher(dispatcherName, dispatcher as ExecutorCoroutineDispatcher)
                }
            } catch (e: Throwable) {
                throw InternalServerException(InfraResponseCode.REDIS_ERROR, "fail to close redis connection", e)
            }
        }
    }
}

data class RedisConfig(
    val host: String, val port: Int = 6379, val password: String?, val rootKeyPrefix: String,
    val client: CoroutineActorConfig,
    val subscribe: SubscribeConfig?
) {

    override fun toString(): String {
        return "url = redis://${if (password != null) "[needPW]@" else ""}$host:$port" +
                " ; rootKeyPrefix = $rootKeyPrefix ; subscribe = ${subscribe != null}"
    }
}

data class SubscribeConfig(
    val patterns: List<String>?,
    val channels: List<String>?,
    val keyspaceNotification: KeyspaceNotificationConfig?
) : ValidateableConfig {

    override fun validate() {
        require(patterns != null || channels != null) {
            "[Redis Subscriber] either patterns or channels should be configured"
        }
    }

    class Builder {

        var patterns: List<String>? = null
        var channels: List<String>? = null
        private var keyspaceNotification: KeyspaceNotificationConfig? = null

        fun keyspaceNotification(block: KeyspaceNotificationConfig.Builder.() -> Unit) {
            keyspaceNotification = KeyspaceNotificationConfig.Builder().apply(block).build()
        }

        fun build(): SubscribeConfig {
            return SubscribeConfig(patterns, channels, keyspaceNotification)
        }
    }

}

data class KeyspaceNotificationConfig(
    val processor: CoroutineActorConfig
) {

    class Builder {

        private lateinit var processor: CoroutineActorConfig

        fun processor(block: CoroutineActorConfig.Builder.() -> Unit) {
            processor = CoroutineActorConfig.Builder().apply(block).build()
        }

        fun build(): KeyspaceNotificationConfig {
            return KeyspaceNotificationConfig(processor)
        }
    }
}