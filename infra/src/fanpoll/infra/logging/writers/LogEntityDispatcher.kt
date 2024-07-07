/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.writers

import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.logging.LogEntity

class LogEntityDispatcher(private val defaultLogWriter: LogWriter? = null) : LogWriter {

    private val logWriters: MutableMap<String, LogWriter> = mutableMapOf()

    fun register(logType: String, logWriter: LogWriter) {
        require(!logWriters.containsKey(logType))

        logWriters[logType] = logWriter
    }

    override suspend fun write(logEntity: LogEntity) {
        val logWriter = logWriters[logEntity.type] ?: defaultLogWriter ?: throw InternalServerException(
            InfraResponseCode.SERVER_CONFIG_ERROR, "logType ${logEntity.type} logWriter is not registered"
        )
        logWriter.write(logEntity)
    }

    override fun shutdown() {
        logWriters.values.forEach { it.shutdown() }
    }
}