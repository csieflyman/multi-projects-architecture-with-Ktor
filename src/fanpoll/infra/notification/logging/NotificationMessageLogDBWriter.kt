/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.logging

import fanpoll.infra.database.custom.lang
import fanpoll.infra.database.sql.UUIDTable
import fanpoll.infra.database.sql.transaction
import fanpoll.infra.logging.LogMessage
import fanpoll.infra.logging.writers.LogWriter
import fanpoll.infra.notification.channel.NotificationChannel
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.`java-time`.timestamp
import org.jetbrains.exposed.sql.insert
import java.util.*

class NotificationMessageLogDBWriter : LogWriter {

    override fun write(message: LogMessage) {
        val dto = message as NotificationMessageLog
        transaction {
            NotificationMessageLogTable.insert {
                it[id] = dto.id
                it[notificationId] = dto.notificationId
                it[eventId] = dto.eventId
                it[type] = dto.type.id
                it[version] = dto.version
                it[channel] = dto.channel
                it[lang] = dto.lang
                it[sendAt] = dto.sendAt
                it[errorMsg] = dto.errorMsg
                it[receivers] = dto.receivers.toString()
                it[content] = dto.content
                it[success] = dto.success
                it[successList] = dto.successList?.toString()
                it[failureList] = dto.failureList?.toString()
                it[invalidRecipientIds] = dto.invalidRecipientIds?.toString()
                it[rspCode] = dto.rspCode
                it[rspMsg] = dto.rspMsg
                it[rspAt] = dto.rspAt
                it[rspTime] = dto.rspTime
                it[rspBody] = dto.rspBody
            }
        }
    }
}

object NotificationMessageLogTable : UUIDTable(name = "infra_notification_message_log") {

    val notificationId = uuid("notification_id")
    val eventId = uuid("event_id")
    val type = varchar("type", 30)
    val version = varchar("version", 5).nullable()
    val channel = enumerationByName("channel", 20, NotificationChannel::class)
    val lang = lang("lang")
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
    val rspTime = long("rsp_time").nullable()
    val rspBody = text("rsp_body").nullable()

    override val naturalKeys: List<Column<out Any>> = listOf(id)
    override val surrogateKey: Column<EntityID<UUID>> = id
}