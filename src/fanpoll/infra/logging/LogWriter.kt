/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.logging

interface LogWriter {

    fun write(message: LogMessage)
}