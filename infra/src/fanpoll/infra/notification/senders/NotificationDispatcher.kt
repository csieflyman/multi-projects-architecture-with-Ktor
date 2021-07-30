/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.senders

import fanpoll.infra.EnvMode
import fanpoll.infra.base.i18n.AvailableLangs
import fanpoll.infra.notification.Notification
import fanpoll.infra.notification.NotificationCategory
import fanpoll.infra.notification.NotificationMessage
import fanpoll.infra.notification.channel.NotificationChannel
import fanpoll.infra.notification.channel.NotificationChannelConfig
import fanpoll.infra.notification.channel.NotificationChannelSender
import fanpoll.infra.notification.channel.email.EmailContent
import fanpoll.infra.notification.channel.push.PushContent
import fanpoll.infra.notification.channel.sms.SMSContent
import fanpoll.infra.notification.i18n.I18nNotificationProjectMessages
import fanpoll.infra.notification.util.NotificationTemplateProcessor
import mu.KotlinLogging
import java.time.Instant

class NotificationDispatcher(
    private val config: NotificationChannelConfig,
    private val envMode: EnvMode,
    private val availableLangs: AvailableLangs,
    private val i18nNotificationProjectMessages: I18nNotificationProjectMessages,
    private val emailSender: NotificationChannelSender? = null,
    private val pushSender: NotificationChannelSender? = null,
    private val smsSender: NotificationChannelSender? = null
) : NotificationSender {

    private val logger = KotlinLogging.logger {}

    private val templateProcessor = NotificationTemplateProcessor(availableLangs)

    override fun send(notification: Notification) {
        logger.debug { "sendNotification: ${notification.debugString()}" }

        if (notification.sendAt == null)
            notification.sendAt = Instant.now()

        if (notification.remote) {
            if (notification.recipients.isEmpty()) {
                logger.warn { "notification recipients is empty! => $notification" }
                return
            }
            TODO("put message into message queue then remote notification service send it")
        } else {
            if (notification.lazyLoad)
                notification.load()

            if (notification.recipients.isEmpty()) {
                logger.warn { "notification recipients is empty! => $notification" }
                return
            }

            notification.recipients.forEach {
                if (it.lang == null)
                    it.lang = notification.type.lang ?: availableLangs.first()
            }

            val notificationMessages = toNotificationMessages(notification)
            sendNotificationMessages(notificationMessages)
        }
    }

    private fun toNotificationMessages(notification: Notification): List<NotificationMessage> {
        val recipientsGroupByLang = notification.recipients.groupBy { it.lang!! }
        val langs = recipientsGroupByLang.keys

        return with(notification) {
            type.channels.flatMap { channel ->
                val sender = when (channel) {
                    NotificationChannel.Email -> {
                        val emailConfig = config.email!!
                        when (type.category) {
                            NotificationCategory.System -> emailConfig.noReplyAddress
                            NotificationCategory.Marketing -> emailConfig.marketingAddress ?: emailConfig.noReplyAddress
                        }
                    }
                    else -> null
                }

                langs.mapNotNull { lang ->
                    val i18nMessages = i18nNotificationProjectMessages.getMessages(notification.type, lang)
                    val langRecipients = recipientsGroupByLang[lang]!!
                    val receivers = when (channel) {
                        NotificationChannel.Email -> langRecipients.mapNotNull { it.email }
                        NotificationChannel.Push -> langRecipients.filter { it.pushTokens != null }.flatMap { it.pushTokens!! }
                        NotificationChannel.SMS -> langRecipients.mapNotNull { it.mobile }
                    }
                    if (receivers.isNotEmpty()) {
                        val templateMessagesArgs = templateArgs.mapValues { entry -> entry.value.toString() }
                        val langContent = when (channel) {
                            NotificationChannel.Email -> {
                                content.email.putIfAbsent(lang, EmailContent()) // when EmailContent only have attachment
                                val emailContent = content.email[lang]!!
                                if (emailContent.subject == null)
                                    emailContent.subject = appendEnvPrefixIfInTest(i18nMessages.getEmailSubject(type, templateMessagesArgs))
                                if (emailContent.body == null)
                                    emailContent.body = templateProcessor.processEmail(type, templateArgs, lang)
                                emailContent
                            }
                            NotificationChannel.Push -> {
                                if (!content.push.containsKey(lang)) {
                                    content.push[lang] = PushContent(
                                        appendEnvPrefixIfInTest(i18nMessages.getPushTitle(type, templateMessagesArgs)),
                                        i18nMessages.getPushBody(type, templateMessagesArgs), contentArgs
                                    )
                                }
                                content.push[lang]
                            }
                            NotificationChannel.SMS -> {
                                if (!content.sms.containsKey(lang)) {
                                    content.sms[lang] = SMSContent(
                                        appendEnvPrefixIfInTest(i18nMessages.getSMSBody(type, templateMessagesArgs))
                                    )
                                }
                                content.sms[lang]
                            }
                        }!!
                        NotificationMessage(id, eventId, type, version, channel, lang, sender, receivers, langContent)
                    } else null
                }
            }
        }
    }

    private fun appendEnvPrefixIfInTest(value: String) = if (envMode != EnvMode.prod) "[${envMode.name}] $value" else value

    private fun sendNotificationMessages(notificationMessages: List<NotificationMessage>) {
        notificationMessages.forEach {
            logger.debug { "sendNotificationMessage: ${it.debugString()}" }
            when (it.channel) {
                NotificationChannel.Email -> sendNotificationMessage(emailSender!!, it)
                NotificationChannel.Push -> sendNotificationMessage(pushSender!!, it)
                NotificationChannel.SMS -> sendNotificationMessage(smsSender!!, it)
            }
        }
    }

    private fun sendNotificationMessage(sender: NotificationChannelSender, message: NotificationMessage) {
        if (message.receivers.size <= sender.maxReceiversPerRequest) {
            message.sendAt = Instant.now()
            sender.send(message)
        } else {
            var start = 0
            while (start < message.receivers.size) {
                val end = (start + sender.maxReceiversPerRequest).coerceAtMost(message.receivers.size)
                val receivers = message.receivers.subList(start, end)
                val subMessage = message.copy(receivers = receivers)
                subMessage.sendAt = Instant.now()
                logger.debug { "sendSubNotificationMessage: ${subMessage.debugString()}" }
                sender.send(subMessage)
                start += sender.maxReceiversPerRequest
            }
        }
    }

    override fun shutdown() {
        logger.info("shutdown NotificationDispatcher...")
        emailSender?.shutdown()
        pushSender?.shutdown()
        smsSender?.shutdown()
        logger.info("shutdown NotificationDispatcher completed")
    }
}