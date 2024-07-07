/*
 * Copyright (c) 2023. fanpoll All rights reserved.
 */

package fanpoll.ops.notification

import fanpoll.infra.auth.authorize
import fanpoll.infra.base.query.DynamicQuery
import fanpoll.infra.base.response.DataResponseDTO
import fanpoll.infra.base.response.respond
import fanpoll.infra.database.exposed.sql.dbExecute
import fanpoll.infra.database.exposed.sql.toList
import fanpoll.infra.database.exposed.util.DynamicDBQuery
import fanpoll.infra.database.exposed.util.ResultRowMappers
import fanpoll.infra.notification.Notification
import fanpoll.infra.notification.NotificationDataLoader
import fanpoll.infra.notification.Recipient
import fanpoll.infra.notification.senders.NotificationSender
import fanpoll.infra.notification.util.DynamicNotification
import fanpoll.infra.openapi.route.post
import fanpoll.ops.OpsAuth
import fanpoll.ops.OpsConst
import fanpoll.ops.OpsKoinContext
import fanpoll.ops.OpsUserType
import fanpoll.ops.user.dtos.UserDTO
import fanpoll.ops.user.repository.exposed.UserTable
import io.ktor.server.application.call
import io.ktor.server.routing.Route
import org.jetbrains.exposed.sql.Database
import org.koin.ktor.ext.inject
import java.util.*

fun Route.sendDynamicNotification() {

    val notificationSender by inject<NotificationSender>()

    authorize(OpsAuth.OpsTeam) {
        post<DynamicNotification.Form, UUID>("/sendDynamicNotification", NotificationOpenApi.SendDynamicNotification) { form ->
            val dynamicNotificationDataDBLoader = OpsKoinContext.koin.get<DynamicNotificationDataDBLoader>()
            val notification = form.toNotification(OpsConst.projectId, dynamicNotificationDataDBLoader)
            notificationSender.send(notification)
            call.respond(DataResponseDTO.uuid(notification.id))
        }
    }
}

class DynamicNotificationDataDBLoader(private val db: Database) : NotificationDataLoader {

    override suspend fun load(notification: Notification) {
        val dynamicNotification = notification as DynamicNotification
        val form = dynamicNotification.dataLoaderArg
        if (form.recipientsQueryFilter != null)
            notification.recipients.addAll(loadRecipients(form.recipientsQueryFilter))
    }

    private suspend fun loadRecipients(userFilters: Map<String, String>?): Set<Recipient> {
        val recipients: MutableSet<Recipient> = mutableSetOf()
        userFilters?.get(OpsUserType.User.name)?.let { loadUserRecipients(it) }?.let { recipients.addAll(it) }
        return recipients
    }

    private suspend fun loadUserRecipients(userFilter: String): Set<Recipient> {
        val predicate = if (userFilter.isNotBlank())
            DynamicDBQuery.convertPredicate(DynamicQuery.parseFilter(userFilter), ResultRowMappers.getMapper(UserDTO::class)!!)
        else null
        return dbExecute(db) {
            val query = UserTable.select(
                UserTable.id, UserTable.account, UserTable.name,
                UserTable.email, UserTable.mobile, UserTable.lang
            ).where { (UserTable.enabled eq true) }
            predicate?.let { query.adjustWhere { it } }
            query.toList(UserDTO::class).map { user ->
                with(user) {
                    Recipient(
                        account!!, OpsUserType.User, id, name!!, lang, email, mobile
                    )
                }
            }.toSet()
        }
    }
}