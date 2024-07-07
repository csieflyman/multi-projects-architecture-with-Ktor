/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.notification

import fanpoll.club.ClubAuth
import fanpoll.club.ClubConst
import fanpoll.club.ClubKoinContext
import fanpoll.club.ClubUserType
import fanpoll.club.user.domain.User
import fanpoll.club.user.dtos.UserDTO
import fanpoll.club.user.repository.exposed.UserTable
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
import fanpoll.infra.notification.channel.push.token.DevicePushTokenRepository
import fanpoll.infra.notification.senders.NotificationSender
import fanpoll.infra.notification.util.DynamicNotification
import fanpoll.infra.openapi.route.post
import io.ktor.server.application.call
import io.ktor.server.routing.Route
import org.jetbrains.exposed.sql.Database
import org.koin.ktor.ext.inject
import java.util.*

fun Route.sendDynamicNotification() {

    val notificationSender by inject<NotificationSender>()

    authorize(ClubAuth.Admin) {

        post<DynamicNotification.Form, UUID>("/sendDynamicNotification", NotificationOpenApi.SendDynamicNotification) { form ->
            val dynamicNotificationDataDBLoader = ClubKoinContext.koin.get<DynamicNotificationDataDBLoader>()
            val notification = form.toNotification(ClubConst.projectId, dynamicNotificationDataDBLoader)
            notificationSender.send(notification)
            call.respond(DataResponseDTO.uuid(notification.id))
        }
    }
}

class DynamicNotificationDataDBLoader(
    private val db: Database,
    private val devicePushTokenRepository: DevicePushTokenRepository
) : NotificationDataLoader {

    override suspend fun load(notification: Notification) {
        val dynamicNotification = notification as DynamicNotification
        val form = dynamicNotification.dataLoaderArg
        if (form.recipientsQueryFilter != null)
            notification.recipients.addAll(loadRecipients(form.recipientsQueryFilter))
    }

    private suspend fun loadRecipients(userFilters: Map<String, String>?): Set<Recipient> {
        val recipients: MutableSet<Recipient> = mutableSetOf()
        userFilters?.get(ClubUserType.User.name)?.let { loadUserRecipients(it) }?.let { recipients.addAll(it) }
        return recipients
    }

    private suspend fun loadUserRecipients(userFilter: String): List<Recipient> {
        val predicate = if (userFilter.isNotBlank())
            DynamicDBQuery.convertPredicate(DynamicQuery.parseFilter(userFilter), ResultRowMappers.getMapper(UserDTO::class)!!)
        else null
        val users = dbExecute(db) {
            val query = UserTable.select(
                UserTable.id, UserTable.account, UserTable.name,
                UserTable.email, UserTable.mobile, UserTable.lang
            ).where { UserTable.enabled eq true }
            predicate?.let { query.adjustWhere { it } }
            query.toList(User::class)
        }
        val userPushTokens = devicePushTokenRepository.getUserPushTokens(users.map { it.id })
        return users.map { user ->
            with(user) {
                Recipient(
                    account!!, ClubUserType.User, id, name!!, lang, email, mobile,
                    userPushTokens[id]?.toSet()
                )
            }
        }.toList()
    }
}