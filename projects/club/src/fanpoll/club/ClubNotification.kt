/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.club

import fanpoll.club.features.ClubUserTable
import fanpoll.club.features.UserDTO
import fanpoll.infra.app.UserDeviceTable
import fanpoll.infra.auth.principal.UserType
import fanpoll.infra.base.query.DynamicQuery
import fanpoll.infra.database.sql.transaction
import fanpoll.infra.database.util.DynamicDBQuery
import fanpoll.infra.database.util.toDTO
import fanpoll.infra.notification.Notification
import fanpoll.infra.notification.NotificationCategory
import fanpoll.infra.notification.NotificationType
import fanpoll.infra.notification.Recipient
import fanpoll.infra.notification.channel.NotificationChannel
import fanpoll.infra.openapi.schema.operation.support.OpenApiIgnore
import kotlinx.serialization.Transient
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.select
import kotlin.reflect.full.memberProperties
import kotlin.reflect.typeOf

object ClubNotification {

    val SendNotification = ClubNotificationType(
        "sendNotification",
        setOf(NotificationChannel.Email, NotificationChannel.Push, NotificationChannel.SMS),
        NotificationCategory.System
    )

    private val notificationType = typeOf<ClubNotificationType>()

    val AllTypes = ClubNotification::class.memberProperties
        .filter { it.returnType == notificationType }
        .map { it.getter.call(this) as ClubNotificationType }
}

class ClubNotificationType(
    name: String,
    channels: Set<NotificationChannel>,
    category: NotificationCategory,
    @Transient @OpenApiIgnore private val lazyLoadBlock: (NotificationType.(Notification) -> Unit)? = null
) : NotificationType(ClubConst.projectId, name, channels, category, null, null, lazyLoadBlock) {

    override fun findRecipients(userFilters: Map<UserType, String>?): Set<Recipient> {
        val userFilter = userFilters?.get(ClubUserType.User.value)?.let {
            if (it.isBlank()) null
            else DynamicDBQuery.convertPredicate(DynamicQuery.parseFilter(it), UserDTO.mapper)
        }

        return transaction {
            val query = ClubUserTable.join(UserDeviceTable, JoinType.LEFT, ClubUserTable.id, UserDeviceTable.userId) {
                UserDeviceTable.enabled eq true
            }.slice(
                ClubUserTable.id, ClubUserTable.account, ClubUserTable.name,
                ClubUserTable.email, ClubUserTable.mobile, ClubUserTable.lang,
                UserDeviceTable.id, UserDeviceTable.pushToken
            ).select { ClubUserTable.enabled eq true }
            userFilter?.let { query.adjustWhere { it } }
            query.toList().toDTO(UserDTO::class).map { user ->
                with(user) {
                    Recipient(
                        account!!, ClubUserType.User.value, id, name, lang, email, mobile,
                        devices?.mapNotNull { it.pushToken }?.toSet()
                    )
                }
            }.toSet()
        }
    }
}