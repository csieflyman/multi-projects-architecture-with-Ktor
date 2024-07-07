/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.message

import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.config.EnvMode
import fanpoll.infra.i18n.AvailableLangs
import fanpoll.infra.i18n.Lang
import fanpoll.infra.notification.Notification
import fanpoll.infra.notification.NotificationCategory
import fanpoll.infra.notification.NotificationType
import fanpoll.infra.notification.Recipient
import fanpoll.infra.notification.channel.NotificationChannel
import fanpoll.infra.notification.channel.NotificationChannelConfig
import fanpoll.infra.notification.channel.NotificationChannelContent
import fanpoll.infra.notification.channel.email.EmailContent
import fanpoll.infra.notification.channel.push.PushContent
import fanpoll.infra.notification.channel.sms.SMSContent
import fanpoll.infra.notification.message.i18n.I18nNotificationMessages
import fanpoll.infra.notification.message.i18n.I18nProjectNotificationMessagesProvider
import io.github.oshai.kotlinlogging.KotlinLogging

class NotificationMessageBuilder(
    private val config: NotificationChannelConfig,
    private val envMode: EnvMode,
    private val availableLangs: AvailableLangs,
    private val i18NProjectNotificationMessagesProvider: I18nProjectNotificationMessagesProvider,
    private val templateProcessor: NotificationTemplateProcessor
) {

    private val logger = KotlinLogging.logger {}
    fun build(notification: Notification): List<NotificationMessage> {
        val langRecipientsMap = notification.recipients.groupBy { it.lang!! }
        val recipientLangs = langRecipientsMap.keys
        val contentLangs = notification.content.channels.map { it.lang }
        checksAndSetRecipientsLangWithContentLang(notification.recipients, contentLangs)
        val langI18nMessagesMap = recipientLangs.associateWith {
            i18NProjectNotificationMessagesProvider.getMessages(notification.projectId, it)
        }

        val contentChannels = notification.content.channels.map { it.channel }
        val typeChannels = notification.type.channels
        if (contentChannels.subtract(typeChannels).isNotEmpty())
            throw InternalServerException(
                InfraResponseCode.DEV_ERROR,
                "NotificationType ${notification.type} doesn't content's channel. content channels => $contentChannels}"
            )
        val channelContentMap = notification.content.channels.associateBy { (it.channel to it.lang) }
        val channelSenderMap = getChannelSenderMap(contentChannels, notification)

        return if (notification.type.broadcast) {
            contentChannels.flatMap { channel ->
                val sender = channelSenderMap[channel]
                recipientLangs.mapNotNull { lang ->
                    val receivers = recipientsToReceivers(langRecipientsMap[lang]!!, channel)
                    if (receivers.isNotEmpty()) {
                        buildNotificationMessage(
                            channel, lang,
                            notification, channelContentMap[channel to lang],
                            sender, receivers,
                            langI18nMessagesMap[lang]!!
                        )
                    } else null
                }
            }
        } else {
            contentChannels.flatMap { channel ->
                val sender = channelSenderMap[channel]
                recipientLangs.flatMap { lang ->
                    val recipients = langRecipientsMap[lang]!!
                    recipients.mapNotNull { recipient ->
                        val receivers = recipientsToReceivers(listOf(recipient), channel)
                        if (receivers.isNotEmpty()) {
                            notification.content.args.putAll(recipient.templateArgs)
                            buildNotificationMessage(
                                channel, lang,
                                notification, channelContentMap[channel to lang],
                                sender, receivers,
                                langI18nMessagesMap[lang]!!
                            )
                        } else null
                    }
                }
            }
        }
    }

    private fun buildNotificationMessage(
        channel: NotificationChannel, lang: Lang,
        notification: Notification, channelContent: NotificationChannelContent?,
        sender: String?, receivers: List<String>,
        i18nMessages: I18nNotificationMessages
    ): NotificationMessage = with(notification) {
        val templateChannelContent = buildChannelContent(
            projectId, type, channel, lang,
            channelContent, content.args, i18nMessages
        )
        NotificationMessage(
            id, projectId, traceId, eventId, type, version, channel, lang,
            sender, receivers, templateChannelContent, sendAt
        )
    }

    private fun buildChannelContent(
        projectId: String, type: NotificationType, channel: NotificationChannel, lang: Lang,
        channelContent: NotificationChannelContent? = null,
        contentTemplateArgs: MutableMap<String, String>,
        i18nMessages: I18nNotificationMessages,
    ): NotificationChannelContent = when (channel) {
        NotificationChannel.Email -> {
            val emailContent = channelContent as? EmailContent ?: EmailContent(lang)
            if (emailContent.subject == null)
                emailContent.subject = i18nMessages.getEmailSubject(type, contentTemplateArgs)
            if (emailContent.body == null)
                emailContent.body = templateProcessor.processEmail(projectId, type, lang, contentTemplateArgs)
            emailContent.subject = appendEnvPrefixIfInTest(emailContent.subject!!)
            emailContent
        }

        NotificationChannel.Push -> {
            val pushContent = channelContent as? PushContent ?: PushContent(
                lang,
                i18nMessages.getPushTitle(type, contentTemplateArgs),
                i18nMessages.getPushBody(type, contentTemplateArgs),
                contentTemplateArgs
            )
            pushContent.body = appendEnvPrefixIfInTest(pushContent.body)
            pushContent
        }

        NotificationChannel.SMS -> {
            val smsContent = channelContent as? SMSContent ?: SMSContent(
                lang,
                i18nMessages.getSMSBody(type, contentTemplateArgs)
            )
            smsContent.body = appendEnvPrefixIfInTest(smsContent.body)
            smsContent
        }
    }

    private fun checksAndSetRecipientsLangWithContentLang(recipients: MutableSet<Recipient>, contentLangs: List<Lang>) {
        val defaultContentLang = contentLangs.firstOrNull { it == availableLangs.first() }
        val langNotMatchRecipients = recipients.filter { !contentLangs.contains(it.lang) }
        if (langNotMatchRecipients.isNotEmpty()) {
            if (defaultContentLang == null)
                throw InternalServerException(
                    InfraResponseCode.DEV_ERROR,
                    "notification content langs doesn't contains default lang and recipient's lang. recipients => " +
                            "${langNotMatchRecipients.map { "${it.name}(${it.lang})(${it.id})" }}"
                )
            else
                langNotMatchRecipients.forEach { it.lang = defaultContentLang }
        }
    }

    private fun getChannelSenderMap(channels: List<NotificationChannel>, notification: Notification) = channels.associateWith { channel ->
        when (channel) {
            NotificationChannel.Email -> {
                val emailConfig = config.email!!
                when (notification.type.category) {
                    NotificationCategory.System -> emailConfig.noReplyAddress
                    NotificationCategory.Marketing -> emailConfig.marketingAddress ?: emailConfig.noReplyAddress
                }
            }

            else -> null
        }
    }

    private fun recipientsToReceivers(recipients: List<Recipient>, channel: NotificationChannel) = when (channel) {
        NotificationChannel.Email -> recipients.mapNotNull { it.email }
        NotificationChannel.Push -> recipients.filter { it.pushTokens != null }.flatMap { it.pushTokens!! }
        NotificationChannel.SMS -> recipients.mapNotNull { it.mobile }
    }

    private fun appendEnvPrefixIfInTest(value: String) = if (envMode != EnvMode.prod) "[${envMode.name}] $value" else value
}