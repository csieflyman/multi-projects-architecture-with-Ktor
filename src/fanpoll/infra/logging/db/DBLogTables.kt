/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.db

import fanpoll.infra.database.UUIDDTOTable
import fanpoll.infra.database.principalSource
import fanpoll.infra.login.LoginResultCode
import fanpoll.infra.notification.channel.NotificationChannel
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.`java-time`.timestamp
import java.util.*

object RequestLogTable : UUIDDTOTable(name = "infra_request_log") {

    val reqId = varchar("req_id", 32)
    val reqTime = timestamp("req_time")
    val api = varchar("api", 255)
    val querystring = text("querystring").nullable()
    val reqBody = text("req_body").nullable()

    val project = varchar("project", 20)
    val function = varchar("function", 30)
    val tag = varchar("tag", 30).nullable()
    val sourceId = principalSource("source", 30) // name "source" conflict
    val tenantId = varchar("tenant_id", 20).nullable()

    val principal = varchar("principal", 36).nullable()
    val runAs = bool("run_as")
    val clientId = varchar("client_id", 36).nullable()
    val clientVersion = varchar("client_version", 10).nullable()
    val ip = varchar("ip", 39).nullable()

    val rspTime = timestamp("rsp_time")
    val reqMillis = long("req_millis")
    val rspStatus = integer("rsp_status")
    val rspBody = text("rsp_body").nullable()

    override val naturalKeys: List<Column<out Any>> = listOf(id)
    override val surrogateKey: Column<EntityID<UUID>> = id
}

object ErrorLogTable : UUIDDTOTable(name = "infra_error_log") {

    val occurAt = timestamp("occur_at")
    val errorCode = char("error_code", 4)
    val errorMsg = text("error_msg").nullable()

    val reqId = varchar("req_id", 32).nullable()
    val reqTime = timestamp("req_time").nullable()
    val api = varchar("api", 255)
    val querystring = text("querystring").nullable()
    val reqBody = text("req_body").nullable()

    val project = varchar("project", 20).nullable()
    val function = varchar("function", 30).nullable()
    val tag = varchar("tag", 30).nullable()
    val sourceId = principalSource("source", 30) // name "source" conflict
    val tenantId = varchar("tenant_id", 20).nullable()

    val principal = varchar("principal", 36).nullable()
    val runAs = bool("run_as")
    val clientId = varchar("client_id", 36).nullable()
    val clientVersion = varchar("client_version", 36).nullable()
    val ip = varchar("ip", 39).nullable()

    val rspTime = timestamp("rsp_time").nullable()
    val reqMillis = long("req_millis").nullable()

    val serviceName = varchar("service_name", 20).nullable()
    val serviceApi = varchar("service_api", 255).nullable()
    val serviceRspCode = varchar("service_rsp_code", 20).nullable()
    val serviceReqBody = text("service_req_body").nullable()
    val serviceRspBody = text("service_rsp_body").nullable()

    override val naturalKeys: List<Column<out Any>> = listOf(id)
    override val surrogateKey: Column<EntityID<UUID>> = id
}

object LoginLogTable : UUIDDTOTable(name = "infra_login_log") {

    val reqTime = timestamp("req_time")
    val resultCode = enumerationByName("result_code", 20, LoginResultCode::class)
    val userId = uuid("user_id")

    val project = varchar("project", 20)
    val sourceId = principalSource("source", 20) // name "source" conflict
    val tenantId = varchar("tenant_id", 20).nullable()
    val clientId = varchar("client_id", 36).nullable()
    val clientVersion = varchar("client_version", 36).nullable()
    val ip = varchar("ip", 39).nullable()

    val sid = char("sid", 120).nullable()

    override val naturalKeys: List<Column<out Any>> = listOf(id)
    override val surrogateKey: Column<EntityID<UUID>> = id
}

object NotificationChannelLogTable : UUIDDTOTable(name = "infra_notification_channel_log") {

    val type = varchar("type", 30)
    val channel = enumeration("channel", NotificationChannel::class)
    val recipients = text("recipients")
    val createTime = timestamp("create_time")
    val sendTime = timestamp("send_time")

    val content = text("content").nullable()
    val successList = text("success_list").nullable()
    val failureList = text("failure_list").nullable()
    val invalidRecipientIds = text("invalid_recipient_ids").nullable()

    val rspCode = varchar("rsp_code", 30).nullable()
    val rspMsg = text("rsp_msg").nullable()
    val rspTime = timestamp("rsp_time").nullable()
    val rspBody = text("rsp_body").nullable()

    override val naturalKeys: List<Column<out Any>> = listOf(id)
    override val surrogateKey: Column<EntityID<UUID>> = id
}