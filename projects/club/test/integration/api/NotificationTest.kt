/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package integration.api

import fanpoll.club.ClubAuth
import fanpoll.club.ClubUserRole
import fanpoll.club.ClubUserType
import fanpoll.club.user.domain.Gender
import fanpoll.club.user.dtos.CreateUserForm
import fanpoll.infra.auth.AuthConst
import fanpoll.infra.auth.provider.UserRunAsAuthProvider
import fanpoll.infra.auth.provider.UserRunAsToken
import fanpoll.infra.database.exposed.sql.dbExecute
import fanpoll.infra.database.exposed.sql.toList
import fanpoll.infra.i18n.Lang
import fanpoll.infra.notification.channel.NotificationChannel
import fanpoll.infra.notification.channel.email.EmailContent
import fanpoll.infra.notification.channel.push.PushContent
import fanpoll.infra.notification.channel.sms.SMSContent
import fanpoll.infra.notification.logging.NotificationMessageLogDTO
import fanpoll.infra.notification.logging.NotificationMessageLogTable
import fanpoll.infra.notification.util.DynamicNotification
import integration.util.dataJsonObject
import integration.util.getRunAsKey
import integration.util.getServiceApiKey
import integration.util.mergeRootPath
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.selectAll
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

val notificationApiTest: suspend FunSpecContainerScope.(HttpClient) -> Unit = { client ->

    val logger = KotlinLogging.logger {}

    lateinit var userId: UUID
    lateinit var runAsToken: UserRunAsToken

    val notificationUser1Form = CreateUserForm(
        "notification-user_1@test.com", "123456",
        true, "notification-user_1",
        Gender.Male, 2000, "notification-user_1@test.com", "0987654321", Lang.zh_TW, setOf(ClubUserRole.Admin)
    )

    test("send multi-notifications") {
        val createUserResponse = client.post(mergeRootPath("/users")) {
            contentType(ContentType.Application.Json)
            header(AuthConst.API_KEY_HEADER_NAME, getServiceApiKey(ClubAuth.RootSource))
            setBody(notificationUser1Form)
        }
        assertEquals(HttpStatusCode.OK, createUserResponse.status)
        val userIdStr = createUserResponse.dataJsonObject()["id"]?.jsonPrimitive?.content
        assertNotNull(userIdStr)

        userId = UUID.fromString(userIdStr)
        runAsToken = UserRunAsToken(ClubUserType.User, userId)

        val sendNotificationForm = DynamicNotification.Form(
            recipientsQueryFilter = mapOf(ClubUserType.User.name to "[account = ${notificationUser1Form.account}]"),
            content = DynamicNotification.ContentForm(
                email = listOf(EmailContent(Lang.zh_TW, "Test Email", "This is a Email test")),
                push = listOf(PushContent(Lang.zh_TW, "Test Push", "This is a Push test", mutableMapOf("data" to "test"))),
                sms = listOf(SMSContent(Lang.zh_TW, "Test SMS"))
            )
        )

        val sendNotificationResponse = client.post(mergeRootPath("/notification/sendDynamicNotification")) {
            contentType(ContentType.Application.Json)
            header(AuthConst.API_KEY_HEADER_NAME, getRunAsKey(ClubAuth.Android))
            header(UserRunAsAuthProvider.RUN_AS_TOKEN_HEADER_NAME, runAsToken.value)
            setBody(sendNotificationForm)
        }
        assertEquals(HttpStatusCode.OK, sendNotificationResponse.status)
        val notificationId = UUID.fromString(sendNotificationResponse.dataJsonObject()["id"]!!.jsonPrimitive.content)

        // This is a fragile integration test because sending notification is asynchronous operation
        delay(500)

        val notificationMessageLogDTOList = dbExecute {
            NotificationMessageLogTable
                .selectAll().where { NotificationMessageLogTable.notificationId eq notificationId }
                .toList(NotificationMessageLogDTO::class)
        }

        // Note: There is no Push channel log because push token is not exist before login
        assertEquals(2, notificationMessageLogDTOList.size)
        assertEquals(1, notificationMessageLogDTOList.filter { it.channel == NotificationChannel.Email }.size)
        assertEquals(1, notificationMessageLogDTOList.filter { it.channel == NotificationChannel.SMS }.size)
    }
}