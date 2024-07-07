/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.notification

import fanpoll.club.ClubUserType
import fanpoll.infra.i18n.Lang
import fanpoll.infra.notification.Recipient
import fanpoll.infra.notification.channel.email.EmailContent
import fanpoll.infra.notification.channel.push.PushContent
import fanpoll.infra.notification.channel.sms.SMSContent
import fanpoll.infra.notification.util.DynamicNotification
import fanpoll.infra.openapi.route.RouteApiOperation
import fanpoll.infra.openapi.schema.Tag
import fanpoll.infra.openapi.schema.operation.definitions.PropertyDef
import fanpoll.infra.openapi.schema.operation.definitions.SchemaDataType

object NotificationOpenApi {

    private val NotificationTag = Tag("notification")

    val SendDynamicNotification = RouteApiOperation("SendDynamicNotification", listOf(NotificationTag)) {
        addRequestExample(
            DynamicNotification.Form(
                recipients = mutableSetOf(Recipient("tester@test.com", name = "tester", email = "tester@test.com")),
                recipientsQueryFilter = mapOf(ClubUserType.User.name to "[account = tester@test.com]"),
                content = DynamicNotification.ContentForm(
                    email = listOf(EmailContent(Lang.zh_TW, "Test Email", "This is a test")),
                    push = listOf(PushContent(Lang.zh_TW, "Test Push", "This is a test", mutableMapOf("data" to "test"))),
                    sms = listOf(SMSContent(Lang.zh_TW, "Test SMS"))
                )
            )
        )
    }

    val NotificationTypeSchema = PropertyDef(
        ClubNotificationType::class.simpleName!!, SchemaDataType.string,
        kClass = ClubNotificationType::class
    ).also { it.enum = ClubNotificationType.entries.map { it.id } }.createRef()
}