/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.writers

import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.logging.LogMessage

class LogMessageDispatcher(private val defaultLogWriter: LogWriter? = null) : LogWriter {

    private val logWriters: MutableMap<String, LogWriter> = mutableMapOf()

    fun register(logType: String, logWriter: LogWriter) {
        require(!logWriters.containsKey(logType))

        logWriters[logType] = logWriter
    }

    override fun write(message: LogMessage) {
        val logWriter = logWriters[message.logType] ?: defaultLogWriter ?: throw InternalServerException(
            InfraResponseCode.SERVER_CONFIG_ERROR, "logType ${message.logType} logWriter is not registered"
        )
        logWriter.write(message)
    }

    override fun shutdown() {
        logWriters.values.forEach { it.shutdown() }
    }
}