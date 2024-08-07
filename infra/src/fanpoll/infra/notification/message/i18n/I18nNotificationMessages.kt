/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.message.i18n

import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.i18n.Lang
import fanpoll.infra.i18n.Messages
import fanpoll.infra.notification.NotificationType
import fanpoll.infra.notification.channel.NotificationChannel

class I18nNotificationMessages(private val messages: Messages) : Messages {

    fun getEmailSubject(
        type: NotificationType,
        args: Map<String, String>? = null
    ): String = getMessage(type, NotificationChannel.Email, "subject", args)

    fun getPushTitle(
        type: NotificationType,
        args: Map<String, String>? = null
    ): String = getMessage(type, NotificationChannel.Push, "title", args)

    fun getPushBody(
        type: NotificationType,
        args: Map<String, String>? = null
    ): String = getMessage(type, NotificationChannel.Push, "body", args)

    fun getSMSBody(
        type: NotificationType,
        args: Map<String, String>? = null
    ): String = getMessage(type, NotificationChannel.SMS, "body", args)

    private fun getMessage(
        type: NotificationType, channel: NotificationChannel, part: String, args: Map<String, String>? = null
    ): String = get("${type.id}.$channel.$part", args)

    override val lang: Lang = messages.lang

    override fun get(key: String, args: Map<String, Any>?): String = messages.get(key, args)
        ?: throw InternalServerException(InfraResponseCode.DEV_ERROR, "notification i18n message key $key is not found")

    override fun isDefined(key: String): Boolean = messages.isDefined(key)
}