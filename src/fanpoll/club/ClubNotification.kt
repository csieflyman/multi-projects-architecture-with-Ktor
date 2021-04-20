/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.club

import fanpoll.club.features.ClubUserTable
import fanpoll.club.features.UserDTO
import fanpoll.infra.app.UserDeviceTable
import fanpoll.infra.controller.EntityDTO
import fanpoll.infra.controller.Form
import fanpoll.infra.database.DynamicDBQuery
import fanpoll.infra.database.myTransaction
import fanpoll.infra.database.toDBQuery
import fanpoll.infra.notification.NotificationPurpose
import fanpoll.infra.notification.NotificationType
import fanpoll.infra.notification.Recipient
import fanpoll.infra.notification.channel.*
import fanpoll.infra.notification.utils.NotificationTemplateProcessor
import fanpoll.infra.report.data.ReportData
import fanpoll.infra.report.data.ReportDataUtils
import fanpoll.infra.report.data.Table
import fanpoll.infra.utils.DateTimeUtils
import fanpoll.infra.utils.DynamicQuery
import fanpoll.infra.utils.I18nUtils
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import java.time.Instant
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

object ClubNotificationTypes {

    val BroadCast = NotificationType(
        ClubConst.projectId, "BroadCast",
        setOf(NotificationChannel.Push), true, NotificationPurpose.Marketing
    ).apply {

        configureBuildChannelMessage { arg ->
            val dto = arg as ClubSendPushNotificationForm
            val recipients = findRecipients(dto.userFilter)
            PushMessage(this, recipients, recipients.flatMap { it.pushTokens!! }.toSet(), content = dto.message)
        }
    }

    val DynamicReport = NotificationType(
        ClubConst.projectId, "DynamicReport",
        setOf(NotificationChannel.Email), false, NotificationPurpose.System
    ).apply {

        configureBuildChannelMessage { arg ->
            val form = arg as ClubDynamicReportForm
            val dtoClass = form.dataType.entityDTOType.classifier as KClass<EntityDTO<*>>
            val dtoList = myTransaction {
                DynamicQuery.from(form.query).toDBQuery(dtoClass).toList(dtoClass)
            }

            val columnIds = ReportDataUtils.getColumnIds(dtoClass)
            val table = Table(form.dataType.name, columnIds)
            dtoList.forEach { table.addRow(ReportDataUtils.toMap(it, columnIds)) }
            val report = ReportData(id, name, mutableListOf(table))

            val queryTime = Instant.now()
            val args = mapOf(
                "dataType" to form.dataType.name,
                "queryTime" to DateTimeUtils.TAIWAN_DATE_TIME_FORMATTER.format(queryTime),
                "query" to form.query
            )
            val subject = I18nUtils.getEmailSubject(this, args)
            val body = NotificationTemplateProcessor.processEmail(this, args)
            val attachment = report.toExcelAttachment(subject)
            val content = EmailMessageContent(subject, body, listOf(attachment))
            EmailMessage(this, to = form.emails, content = content)
        }
    }

    fun all(): List<NotificationType> = listOf()

    private fun findRecipients(userFilterDsl: String? = null): Set<Recipient> {
        val userFilter = userFilterDsl?.let {
            DynamicDBQuery.convertPredicate(DynamicQuery.parseFilter(it), UserDTO.mapper)
        }

        return myTransaction {
            val query = ClubUserTable.join(UserDeviceTable, JoinType.LEFT, ClubUserTable.id, UserDeviceTable.userId)
                .slice(ClubUserTable.id, ClubUserTable.account, UserDeviceTable.id, UserDeviceTable.pushToken)
                .select {
                    (ClubUserTable.enabled eq true) and (UserDeviceTable.enabled eq true) and (UserDeviceTable.pushToken.isNotNull())
                }
            userFilter?.let { query.adjustWhere { it } }

            query.groupBy { it[ClubUserTable.id].value }.mapValues { entry ->
                val account = entry.value[0][ClubUserTable.account]
                val tokens = entry.value.map { it[UserDeviceTable.pushToken]!! }.toSet()
                Recipient(ClubUserType.User.value, entry.key, account, pushTokens = tokens)
            }.values.toSet()
        }
    }
}

@Serializable
class ClubSendPushNotificationForm(
    val message: PushMessageContent,
    val userFilter: String? = null
) : Form<ClubSendPushNotificationForm>()

@Serializable
class ClubDynamicReportForm(
    val dataType: ClubDynamicReportDataType,
    val query: String,
    val emails: Set<String>
) : Form<ClubDynamicReportForm>()

enum class ClubDynamicReportDataType(val entityDTOType: KType) {
    ClubUser(typeOf<UserDTO>())
}