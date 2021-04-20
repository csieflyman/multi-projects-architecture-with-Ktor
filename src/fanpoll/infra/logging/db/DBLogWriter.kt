/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.db

import fanpoll.infra.logging.*
import fanpoll.infra.notification.channel.NotificationChannelLog
import mu.KotlinLogging
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

class DBLogWriter : LogWriter {

    private val logger = KotlinLogging.logger {}

    override fun write(message: LogMessage) {
        try {
            transaction {
                when (message.type) {
                    LogType.REQUEST -> writeRequestLog(message.dto as RequestLogDTO)
                    LogType.SERVER_ERROR -> writeErrorLog(message.dto as ErrorLogDTO)
                    LogType.LOGIN -> writeLoginLog(message.dto as LoginLogDTO)
                    LogType.NOTIFICATION, LogType.NOTIFICATION_ERROR -> writeNotificationChannelLog(message.dto as NotificationChannelLog)
                }
            }
        } catch (e: Throwable) {
            logger.error("[DBLogWriter] fail to logging ${message.type.name} => ${message.content()}", e)
        }

    }

    private fun writeRequestLog(dto: RequestLogDTO) {
        RequestLogTable.insert {
            it[reqId] = dto.reqId
            it[reqTime] = dto.reqTime
            it[api] = dto.api
            it[querystring] = dto.querystring
            it[reqBody] = dto.reqBody
            it[project] = dto.project
            it[function] = dto.function
            it[tag] = dto.tag
            it[sourceId] = dto.source
            it[tenantId] = dto.tenantId
            it[principal] = dto.principal
            it[runAs] = dto.runAs
            it[clientId] = dto.clientId
            it[clientVersion] = dto.clientVersion
            it[ip] = dto.ip
            it[rspTime] = dto.rspTime
            it[reqMillis] = dto.reqMillis
            it[rspStatus] = dto.rspStatus
            it[rspBody] = dto.rspBody
        }
    }

    private fun writeErrorLog(dto: ErrorLogDTO) {
        ErrorLogTable.insert {
            it[occurAt] = dto.occurAt
            it[errorCode] = dto.errorCode
            it[errorMsg] = dto.errorMsg
            it[reqId] = dto.reqId
            it[reqTime] = dto.reqTime
            it[api] = dto.api
            it[querystring] = dto.querystring
            it[reqBody] = dto.reqBody
            it[project] = dto.project
            it[function] = dto.function
            it[tag] = dto.tag
            it[sourceId] = dto.source
            it[tenantId] = dto.tenantId
            it[principal] = dto.principal
            it[runAs] = dto.runAs
            it[clientId] = dto.clientId
            it[clientVersion] = dto.clientVersion
            it[ip] = dto.ip
            it[rspTime] = dto.rspTime
            it[reqMillis] = dto.reqMillis
            it[serviceName] = dto.serviceName
            it[serviceApi] = dto.serviceApi
            it[serviceRspCode] = dto.serviceRspCode
            it[serviceReqBody] = dto.serviceReqBody
            it[serviceRspBody] = dto.serviceRspBody
        }
    }

    private fun writeLoginLog(dto: LoginLogDTO) {
        LoginLogTable.insert {
            it[reqTime] = dto.reqTime
            it[resultCode] = dto.resultCode
            it[userId] = dto.userId
            it[project] = dto.project
            it[sourceId] = dto.source
            it[tenantId] = dto.tenantId
            it[clientId] = dto.clientId
            it[clientVersion] = dto.clientVersion
            it[ip] = dto.ip
            it[sid] = dto.sid
        }
    }

    private fun writeNotificationChannelLog(dto: NotificationChannelLog) {
        NotificationChannelLogTable.insert {
            it[id] = dto.id
            it[type] = dto.type
            it[channel] = dto.channel
            it[recipients] = dto.recipients.toString()
            it[createTime] = dto.createTime
            it[sendTime] = dto.sendTime
            it[content] = dto.content
            it[successList] = dto.successList?.toString()
            it[failureList] = dto.failureList?.toString()
            it[invalidRecipientIds] = dto.invalidRecipientIds?.toString()
            it[rspCode] = dto.rspCode
            it[rspMsg] = dto.rspMsg
            it[rspTime] = dto.rspTime
            it[rspBody] = dto.rspBody
        }
    }
}