/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.logging

import fanpoll.infra.database.exposed.sql.dbExecute
import fanpoll.infra.database.exposed.sql.langColumn
import fanpoll.infra.logging.LogEntity
import fanpoll.infra.logging.writers.LogWriter
import fanpoll.infra.notification.channel.NotificationChannel
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.duration
import org.jetbrains.exposed.sql.javatime.timestamp

class NotificationMessageLogDBWriter : LogWriter {

    override suspend fun write(logEntity: LogEntity) {
        val messageLog = logEntity as NotificationMessageLog
        dbExecute {
            NotificationMessageLogTable.insert {
                it[id] = messageLog.id
                it[notificationId] = messageLog.notificationId
                it[traceId] = messageLog.traceId
                it[eventId] = messageLog.eventId
                it[type] = messageLog.notificationType.id
                it[version] = messageLog.version
                it[channel] = messageLog.channel
                it[lang] = messageLog.lang
                it[sendAt] = messageLog.sendAt
                it[errorMsg] = messageLog.errorMsg
                it[receivers] = messageLog.receivers.toString()
                it[content] = messageLog.content
                it[success] = messageLog.success
                it[successList] = messageLog.successList?.toString()
                it[failureList] = messageLog.failureList?.toString()
                it[invalidRecipientIds] = messageLog.invalidRecipientIds?.toString()
                it[rspCode] = messageLog.rspCode
                it[rspMsg] = messageLog.rspMsg
                it[rspAt] = messageLog.rspAt
                it[duration] = messageLog.duration
                it[rspBody] = messageLog.rspBody
            }
        }
    }
}

object NotificationMessageLogTable : UUIDTable(name = "infra_notification_message_log") {

    val notificationId = uuid("notification_id")
    val traceId = char("trace_id", 32).nullable()
    val eventId = uuid("event_id")
    val type = varchar("type", 30)
    val version = varchar("version", 5).nullable()
    val channel = enumerationByName("channel", 20, NotificationChannel::class)
    val lang = langColumn("lang")
    val sendAt = timestamp("send_at").nullable()
    val errorMsg = text("error_msg").nullable()
    val receivers = text("receivers")
    val content = text("content").nullable()

    val success = bool("success")
    val successList = text("success_list").nullable()
    val failureList = text("failure_list").nullable()
    val invalidRecipientIds = text("invalid_recipient_ids").nullable()

    val rspCode = varchar("rsp_code", 30).nullable()
    val rspMsg = text("rsp_msg").nullable()
    val rspAt = timestamp("rsp_at").nullable()
    val duration = duration("duration").nullable()
    val rspBody = text("rsp_body").nullable()
}