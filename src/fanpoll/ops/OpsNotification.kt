/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.ops

import fanpoll.infra.auth.principal.UserType
import fanpoll.infra.base.entity.EntityDTO
import fanpoll.infra.base.form.Form
import fanpoll.infra.base.i18n.Lang
import fanpoll.infra.base.query.DynamicQuery
import fanpoll.infra.base.util.DateTimeUtils
import fanpoll.infra.database.sql.transaction
import fanpoll.infra.database.util.DynamicDBQuery
import fanpoll.infra.database.util.toDBQuery
import fanpoll.infra.database.util.toDTO
import fanpoll.infra.notification.Notification
import fanpoll.infra.notification.NotificationCategory
import fanpoll.infra.notification.NotificationType
import fanpoll.infra.notification.Recipient
import fanpoll.infra.notification.channel.NotificationChannel
import fanpoll.infra.notification.channel.email.EmailContent
import fanpoll.infra.notification.channel.email.toExcelAttachment
import fanpoll.infra.openapi.schema.operation.support.OpenApiIgnore
import fanpoll.infra.report.data.ReportData
import fanpoll.infra.report.data.ReportDataUtils
import fanpoll.infra.report.data.Table
import fanpoll.ops.features.OpsUserTable
import fanpoll.ops.features.UserDTO
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import mu.KotlinLogging
import org.jetbrains.exposed.sql.select
import java.time.Instant
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.typeOf

object OpsNotification {

    private val logger = KotlinLogging.logger {}

    val SendNotification = OpsNotificationType("sendNotification")

    val DataReport = OpsNotificationType("dataReport") { notification ->
        val form = notification.lazyLoadArg as DataReportForm
        requireNotNull(form.email)
        notification.recipients.add(Recipient(form.email!!, email = form.email))

        val dtoClass = form.dataType.entityDTOType.classifier as KClass<EntityDTO<*>>
        val dtoList = transaction {
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
        notification.templateArgs.putAll(args)

        val fileName = "${this.name}_${args["dataType"]}_${args["queryTime"]}"
        val attachment = report.toExcelAttachment(fileName)
        notification.content.email[lang!!] = EmailContent(attachments = listOf(attachment))
    }

    private val notificationType = typeOf<OpsNotificationType>()

    val AllTypes = OpsNotification::class.memberProperties
        .filter { it.returnType == notificationType }
        .map { it.getter.call(this) as OpsNotificationType }
}

@Serializable
class DataReportForm(
    val dataType: DataReportDataType,
    val query: String,
    var email: String? = null
) : Form<DataReportForm>()

enum class DataReportDataType(val entityDTOType: KType) {
    OpsUser(typeOf<UserDTO>()), ClubUser(typeOf<fanpoll.club.user.UserDTO>())
}

class OpsNotificationType(
    name: String,
    @Transient @OpenApiIgnore private val lazyLoadBlock: (NotificationType.(Notification) -> Unit)? = null
) : NotificationType(
    OpsConst.projectId, name, setOf(NotificationChannel.Email),
    NotificationCategory.System, null, Lang.SystemDefault, lazyLoadBlock
) {

    override fun findRecipients(userFilters: Map<UserType, String>?): Set<Recipient> {
        val userFilter = userFilters?.get(OpsUserType.User.value)?.let {
            if (it.isBlank()) null
            else DynamicDBQuery.convertPredicate(DynamicQuery.parseFilter(it), UserDTO.mapper)
        }

        return transaction {
            val query = OpsUserTable.slice(
                OpsUserTable.id, OpsUserTable.account, OpsUserTable.name,
                OpsUserTable.email, OpsUserTable.mobile, OpsUserTable.lang
            ).select { (OpsUserTable.enabled eq true) }
            userFilter?.let { query.adjustWhere { it } }
            query.toList().toDTO(UserDTO::class).map { user ->
                with(user) {
                    Recipient(
                        account!!, OpsUserType.User.value, id, name, lang, email, mobile
                    )
                }
            }.toSet()
        }
    }
}