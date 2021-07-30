/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.writers

import fanpoll.infra.logging.LogMessage

interface LogWriter {

    fun write(message: LogMessage)

    fun shutdown() {}
}