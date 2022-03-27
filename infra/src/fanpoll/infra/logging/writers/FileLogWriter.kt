/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.writers

import fanpoll.infra.logging.LogLevel
import fanpoll.infra.logging.LogMessage
import mu.KotlinLogging

class FileLogWriter : LogWriter {

    private val logger = KotlinLogging.logger {}

    override fun write(logEntity: LogEntity) {
        val messageString = "[${logEntity.type}] => ${logEntity.toJson()}"
        when (logEntity.level) {
            LogLevel.DEBUG -> logger.debug { messageString }
            LogLevel.INFO -> logger.info { messageString }
            LogLevel.WARN -> logger.warn { messageString }
            LogLevel.ERROR, LogLevel.FATAL -> logger.error { messageString }
        }
    }
}