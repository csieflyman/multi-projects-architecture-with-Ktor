/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.redis

import fanpoll.infra.base.async.CoroutineUtils
import fanpoll.infra.base.async.ThreadPoolConfig
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.koin.KoinApplicationShutdownManager
import fanpoll.infra.logging.writers.LogWriter
import fanpoll.infra.redis.ktorio.RedisClient
import fanpoll.infra.redis.ktorio.commands.ping
import fanpoll.infra.redis.ktorio.commands.psubscribe
import fanpoll.infra.redis.ktorio.commands.quit
import fanpoll.infra.redis.ktorio.commands.subscribe
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.server.application.Application
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.koin.ktor.ext.get
import kotlin.system.measureTimeMillis

private val logger = KotlinLogging.logger {}

//private lateinit var config: RedisConfig

private lateinit var client: RedisClient
private const val dispatcherName = "Redis"
private lateinit var dispatcher: CoroutineDispatcher

private lateinit var subscribeClient: RedisClient
private const val subscribeDispatcherName = "Redis-Subscriber"
private lateinit var subscribeDispatcher: CoroutineDispatcher

private var keyspaceNotificationListener: RedisKeyspaceNotificationListener? = null

fun Application.loadRedisModule(redisConfig: RedisConfig) = loadKoinModules(module(createdAtStart = true) {
    val logWriter = get<LogWriter>()

    initClient(redisConfig)

    if (redisConfig.subscribe != null) {
        initSubscriber(redisConfig, logWriter)
    }

    single { client }
    if (keyspaceNotificationListener != null)
        single { keyspaceNotificationListener }
    KoinApplicationShutdownManager.register { shutdown(redisConfig) }
})

private fun initClient(config: RedisConfig) {
    logger.info { "========== init Redis Client... ==========" }
    try {
        dispatcher = if (config.client.dispatcher != null)
            CoroutineUtils.createDispatcher(dispatcherName, config.client.dispatcher)
        else Dispatchers.IO

        logger.info { "connect to redis => $config" }
        client = RedisClient(
            address = InetSocketAddress(config.host, config.port),
            password = config.password,
            maxConnections = config.client.coroutines,
            dispatcher = dispatcher, rootKeyPrefix = config.rootKeyPrefix
        )

        runBlocking {
            logger.info { "ping..." }
            val latency = measureTimeMillis {
                client.ping()?.let {
                    logger.info { it }
                }
            }
            logger.info { "ping latency = $latency milliseconds" }
        }
    } catch (e: Throwable) {
        throw InternalServerException(InfraResponseCode.REDIS_ERROR, "fail to init redis client", e)
    }
    logger.info { "========== init Redis Client completed ==========" }
}

private fun initSubscriber(config: RedisConfig, logWriter: LogWriter) {
    logger.info { "========== init Redis PubSub subscriber... ==========" }
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
    logger.info { "========== init Redis PubSub subscriber completed ==========" }
}

private fun shutdown(redisConfig: RedisConfig) {
    if (redisConfig.subscribe != null)
        closePubSubClient()
    closeClient()
}

private fun closePubSubClient() {
    try {
        runBlocking {
            logger.info { "close Redis PubSub subscriber connection..." }
            subscribeClient.quit()
            logger.info { "Redis PubSub subscriber connection closed" }
        }

        CoroutineUtils.closeDispatcher(subscribeDispatcherName, subscribeDispatcher as ExecutorCoroutineDispatcher)

        keyspaceNotificationListener?.shutdown()
    } catch (e: Throwable) {
        throw InternalServerException(InfraResponseCode.REDIS_ERROR, "fail to close Redis PubSub subscriber connection", e)
    }
}

private fun closeClient() {
    try {
        runBlocking {
            logger.info { "close redis connection..." }
            client.quit()
            logger.info { "redis connection closed" }
        }

        if (dispatcher is ExecutorCoroutineDispatcher) {
            CoroutineUtils.closeDispatcher(dispatcherName, dispatcher as ExecutorCoroutineDispatcher)
        }
    } catch (e: Throwable) {
        throw InternalServerException(InfraResponseCode.REDIS_ERROR, "fail to close redis connection", e)
    }
}