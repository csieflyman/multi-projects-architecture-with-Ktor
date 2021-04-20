/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.logging

import fanpoll.infra.notification.channel.NotificationChannelLog
import fanpoll.infra.utils.json
import kotlinx.serialization.json.JsonElement

enum class LogType {

    REQUEST, SERVER_ERROR, LOGIN, NOTIFICATION, NOTIFICATION_ERROR;

    fun isError(): Boolean {
        return this == SERVER_ERROR || this == NOTIFICATION_ERROR
    }
}

data class LogMessage(val type: LogType, val dto: Any) {

    fun content(): JsonElement {
        return when (type) {
            LogType.REQUEST -> json.encodeToJsonElement(RequestLogDTO.serializer(), dto as RequestLogDTO)
            LogType.SERVER_ERROR -> json.encodeToJsonElement(ErrorLogDTO.serializer(), dto as ErrorLogDTO)
            LogType.LOGIN -> json.encodeToJsonElement(LoginLogDTO.serializer(), dto as LoginLogDTO)
            LogType.NOTIFICATION, LogType.NOTIFICATION_ERROR ->
                json.encodeToJsonElement(NotificationChannelLog.serializer(), dto as NotificationChannelLog)
        }
    }
}