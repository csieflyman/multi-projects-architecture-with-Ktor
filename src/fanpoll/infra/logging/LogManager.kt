/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.logging

import fanpoll.infra.InternalServerErrorException
import fanpoll.infra.ResponseCode
import fanpoll.infra.ServerConfig
import fanpoll.infra.logging.db.DBLogWriter
import fanpoll.infra.logging.kinesis.KinesisLogWriter
import fanpoll.infra.utils.CoroutineUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import mu.KotlinLogging

object LogManager {

    private val logger = KotlinLogging.logger {}

    private lateinit var logWriter: LogWriter

    private const val dispatcherName = "LOG"
    private lateinit var dispatcher: CoroutineDispatcher
    private lateinit var channel: SendChannel<LogMessage>
    private lateinit var coroutineScope: CoroutineScope

    suspend fun write(data: LogMessage) {
        channel.send(data)
    }

    fun writeAsync(data: LogMessage) {
        runBlocking {
            write(data)
        }
    }

    fun init(loggingConfig: LoggingConfig, serverConfig: ServerConfig) {
        logger.info("========== init LogManager... ==========")
        try {
            logWriter = when (loggingConfig.storage) {
                StorageType.DB -> DBLogWriter()
                StorageType.Kinesis -> KinesisLogWriter(loggingConfig.kinesis!!, serverConfig)
            }

            dispatcher = if (loggingConfig.coroutine.dispatcher != null)
                CoroutineUtils.createDispatcher(dispatcherName, loggingConfig.coroutine.dispatcher)
            else Dispatchers.IO

            val context = dispatcher // TODO coroutine exception handling
            coroutineScope = CoroutineScope(context)
            channel = CoroutineUtils.createActor(
                dispatcherName, loggingConfig.coroutine.coroutines,
                coroutineScope, logWriter::write
            )
        } catch (e: Throwable) {
            throw InternalServerErrorException(ResponseCode.LOG_ERROR, "fail to init LogManager", e)
        }
        logger.info("========== init LogManager completed ==========")
    }

    fun shutdown() {
        logger.info("shutdown LogManager...")
        closeCoroutine()
        logger.info("shutdown LogManager completed")
    }

    private fun closeCoroutine() {
        CoroutineUtils.closeChannel(dispatcherName, channel)
        coroutineScope.cancel(dispatcherName)
        if (dispatcher is ExecutorCoroutineDispatcher) {
            CoroutineUtils.closeDispatcher(dispatcherName, dispatcher as ExecutorCoroutineDispatcher)
        }
    }
}