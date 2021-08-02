/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.writers

import fanpoll.infra.base.async.CoroutineActor
import fanpoll.infra.base.async.CoroutineActorConfig
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.logging.LogMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel

class LogMessageCoroutineActor(
    coroutineActorConfig: CoroutineActorConfig,
    private val logWriter: LogWriter
) : LogWriter {

    private val actorName = "LogWriterActor"

    private val actor: CoroutineActor<LogMessage> = CoroutineActor(
        actorName, Channel.UNLIMITED,
        coroutineActorConfig, Dispatchers.IO,
        this::execute
    )

    override fun write(message: LogMessage) {
        actor.sendToUnlimitedChannel(message, InfraResponseCode.LOG_ERROR) // non-blocking by Channel.UNLIMITED
    }

    private fun execute(message: LogMessage) {
        logWriter.write(message)
    }

    override fun shutdown() {
        actor.close()
        logWriter.shutdown()
    }
}