/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.writers

import fanpoll.infra.base.async.CoroutineActor
import fanpoll.infra.base.async.CoroutineActorConfig
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.logging.LogEntity
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel

class LogEntityCoroutineActor(
    coroutineActorConfig: CoroutineActorConfig,
    private val logWriter: LogWriter
) : LogWriter {

    private val logger = KotlinLogging.logger {}

    private val actorName = "LogWriterActor"

    private val actor: CoroutineActor<LogEntity> = CoroutineActor(
        actorName, Channel.UNLIMITED,
        coroutineActorConfig, Dispatchers.IO,
        this::execute
    )

    override suspend fun write(logEntity: LogEntity) {
        actor.sendToUnlimitedChannel(logEntity, InfraResponseCode.LOG_ERROR) // non-blocking by Channel.UNLIMITED
    }

    private suspend fun execute(logEntity: LogEntity) {
        try {
            logWriter.write(logEntity)
        } catch (e: Throwable) {
            logger.error(e) { "$actorName execute error => $logEntity" }
        }
    }

    override fun shutdown() {
        actor.close()
        logWriter.shutdown()
    }
}