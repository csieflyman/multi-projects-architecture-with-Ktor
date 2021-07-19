/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.logging

import fanpoll.infra.base.response.ResponseCodeType

enum class LogLevel {

    DEBUG, INFO, WARN, ERROR, FATAL
}

fun ResponseCodeType.logLevel() = when (this) {
    ResponseCodeType.SUCCESS -> LogLevel.DEBUG
    ResponseCodeType.CLIENT_INFO -> LogLevel.INFO
    ResponseCodeType.CLIENT_ERROR -> LogLevel.INFO
    ResponseCodeType.SERVER_ERROR -> LogLevel.ERROR
}