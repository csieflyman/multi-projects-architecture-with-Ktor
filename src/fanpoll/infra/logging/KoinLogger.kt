/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.logging

import mu.KotlinLogging
import org.koin.core.logger.Level
import org.koin.core.logger.Logger
import org.koin.core.logger.MESSAGE

class KoinLogger(level: Level = Level.INFO) : Logger(level) {

    private val logger = KotlinLogging.logger("[Koin]")

    override fun log(level: Level, msg: MESSAGE) {
        when (level) {
            Level.DEBUG -> logger.debug { msg }
            Level.INFO -> logger.info { msg }
            Level.ERROR -> logger.error { msg }
            Level.NONE -> logger.trace { msg }
        }
    }
}