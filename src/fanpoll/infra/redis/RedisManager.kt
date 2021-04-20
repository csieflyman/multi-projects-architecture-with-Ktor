/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.redis

import fanpoll.infra.InternalServerErrorException
import fanpoll.infra.ResponseCode
import fanpoll.infra.redis.ktorio.RedisClient
import fanpoll.infra.redis.ktorio.commands.*
import fanpoll.infra.utils.CoroutineUtils
import fanpoll.infra.utils.DispatcherConfig
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.net.InetSocketAddress
import kotlin.system.measureTimeMillis

object RedisManager {

    private val logger = KotlinLogging.logger {}

    lateinit var client: RedisClient
        private set
    private lateinit var config: RedisConfig

    private const val dispatcherName = "Redis"
    private lateinit var dispatcher: CoroutineDispatcher

    private lateinit var pubSubClient: RedisClient
    private const val pubSubDispatcherName = "Redis-PubSub"
    private lateinit var pubSubDispatcher: CoroutineDispatcher

    private const val keyspaceNotificationDispatcherName = "Redis-KeyspaceNotification"
    private lateinit var keyspaceNotificationDispatcher: CoroutineDispatcher
    private val keyspaceNotificationBlocks: MutableList<suspend (RedisPubSub.KeyspaceNotification) -> Unit> = mutableListOf()

    fun init(config: RedisConfig) {
        logger.info("========== init RedisManager... ==========")
        RedisManager.config = config
        try {
            initClient(config)

            runBlocking {
                testLatency()
            }

            if (config.pubSub != null) {
                initPubSub(config)
            }
        } catch (e: Throwable) {
            throw InternalServerErrorException(ResponseCode.REDIS_ERROR, "fail to init RedisManager", e)
        }
        logger.info("========== init RedisManager completed ==========")
    }

    private fun initClient(config: RedisConfig) {
        dispatcher = if (config.client.dispatcher != null)
            CoroutineUtils.createDispatcher(dispatcherName, config.client.dispatcher)
        else Dispatchers.IO

        logger.info("connect to redis => $config")
        client = RedisClient(
            address = InetSocketAddress(config.host, config.port!!),
            password = config.password,
            maxConnections = config.client.coroutines,
            dispatcher = dispatcher, rootKeyPrefix = config.rootKeyPrefix
        )
    }

    private suspend fun testLatency() {
        logger.info("ping...")
        val latency = measureTimeMillis {
            client.ping()?.let {
                logger.info(it)
            }
        }
        logger.info("ping latency = $latency milliseconds")
    }

    fun shutdown() {
        logger.info("shutdown RedisManager...")
        closePubSubClient()
        closeClient()
        logger.info("shutdown RedisManager completed")
    }

    private fun closeClient() {
        try {
            runBlocking {
                logger.info("close Redis connection...")
                client.quit()
                logger.info("Redis connection closed")
            }

            if (dispatcher is ExecutorCoroutineDispatcher) {
                CoroutineUtils.closeDispatcher(dispatcherName, dispatcher as ExecutorCoroutineDispatcher)
            }
        } catch (e: Throwable) {
            throw InternalServerErrorException(ResponseCode.REDIS_ERROR, "could not close redis connection", e)
        }
    }

    private fun closePubSubClient() {
        if (config.pubSub != null) {
            runBlocking {
                logger.info("close Redis PubSub connection...")
                pubSubClient.quit()
                logger.info("Redis PubSub connection closed")
            }

            CoroutineUtils.closeDispatcher(pubSubDispatcherName, pubSubDispatcher as ExecutorCoroutineDispatcher)

            if (config.pubSub!!.keyspaceNotification != null && keyspaceNotificationDispatcher is ExecutorCoroutineDispatcher) {
                CoroutineUtils.closeDispatcher(
                    keyspaceNotificationDispatcherName,
                    keyspaceNotificationDispatcher as ExecutorCoroutineDispatcher
                )
            }
        }
    }

    private fun initPubSub(config: RedisConfig) {
        logger.info("========== init Redis PubSub... ==========")

        pubSubDispatcher = CoroutineUtils.createDispatcher(pubSubDispatcherName, DispatcherConfig(fixedPoolSize = 1))
        pubSubClient = RedisClient(
            address = InetSocketAddress(config.host, config.port!!),
            password = config.password,
            maxConnections = 1,
            dispatcher = pubSubDispatcher, rootKeyPrefix = config.rootKeyPrefix
        )

        with(config.pubSub!!) {
            val redisPubSub = runBlocking {
                patterns?.let { pubSubClient.psubscribe(*it.toTypedArray()) }
                    ?: channels?.let { pubSubClient.subscribe(*it.toTypedArray()) }
            }!!

            if (keyspaceNotification != null) {
                initKeySpaceNotification(keyspaceNotification, redisPubSub)
            }
        }

        logger.info("========== init Redis PubSub completed ==========")
    }

    private fun initKeySpaceNotification(config: KeyspaceNotificationConfig, redisPubSub: RedisPubSub) {
        logger.info("========== init Redis KeyspaceNotification... ==========")

        keyspaceNotificationDispatcher = if (config.coroutine.dispatcher != null)
            CoroutineUtils.createDispatcher(keyspaceNotificationDispatcherName, config.coroutine.dispatcher)
        else Dispatchers.IO

        val coroutineScope = CoroutineScope(keyspaceNotificationDispatcher) // TODO coroutine exception handling
        val actor = CoroutineUtils.createActor(
            keyspaceNotificationDispatcherName, config.coroutine.coroutines,
            coroutineScope, RedisManager::processKeyspaceNotification
        )

        coroutineScope.launch {
            val channel = redisKeyspaceNotificationChannel(redisPubSub)
            for (e in channel) {
                actor.send(e)
            }
        }

        logger.info("========== init Redis KeyspaceNotification completed ==========")
    }

    private suspend fun processKeyspaceNotification(data: RedisPubSub.KeyspaceNotification) {
        keyspaceNotificationBlocks.forEach { it(data) }
    }

    fun subscribeKeyspaceNotification(block: suspend (RedisPubSub.KeyspaceNotification) -> Unit) {
        keyspaceNotificationBlocks.add(block)
    }
}