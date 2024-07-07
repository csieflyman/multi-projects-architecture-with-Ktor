/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.notification

import fanpoll.infra.i18n.Lang
import fanpoll.infra.notification.Recipient
import fanpoll.infra.notification.channel.email.EmailContent
import fanpoll.infra.notification.channel.sms.SMSContent
import fanpoll.infra.notification.util.DynamicNotification
import fanpoll.infra.openapi.route.RouteApiOperation
import fanpoll.infra.openapi.schema.Tag
import fanpoll.infra.openapi.schema.operation.definitions.PropertyDef
import fanpoll.infra.openapi.schema.operation.definitions.SchemaDataType
import fanpoll.ops.OpsUserType

object NotificationOpenApi {

    private val NotificationTag = Tag("notification")

    val SendDynamicNotification = RouteApiOperation("SendDynamicNotification", listOf(NotificationTag)) {

        addRequestExample(
            DynamicNotification.Form(
                recipients = mutableSetOf(Recipient("tester@test.com", name = "tester", email = "tester@test.com")),
                recipientsQueryFilter = mapOf(OpsUserType.User.name to "[account = tester@test.com]"),
                content = DynamicNotification.ContentForm(
                    email = listOf(EmailContent(Lang.zh_TW, "Test Email", "This is a test")),
                    sms = listOf(SMSContent(Lang.zh_TW, "Test SMS"))
                )
            )
        )
    }

    val NotificationTypeSchema = PropertyDef(
        OpsNotificationType::class.simpleName!!, SchemaDataType.string,
        kClass = OpsNotificationType::class
    ).also { it.enum = OpsNotificationType.entries.map { it.id } }.createRef()
}