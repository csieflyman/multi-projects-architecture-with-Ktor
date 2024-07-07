/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.notification

import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.config.EnvMode
import fanpoll.infra.database.exposed.util.ResultRowMapper
import fanpoll.infra.database.exposed.util.ResultRowMappers
import fanpoll.infra.i18n.AvailableLangs
import fanpoll.infra.koin.KoinApplicationShutdownManager
import fanpoll.infra.logging.LogDestination
import fanpoll.infra.logging.writers.FileLogWriter
import fanpoll.infra.logging.writers.LogEntityDispatcher
import fanpoll.infra.logging.writers.LogWriter
import fanpoll.infra.logging.writers.LokiLogWriter
import fanpoll.infra.notification.channel.MockNotificationChannelSender
import fanpoll.infra.notification.channel.NotificationChannel
import fanpoll.infra.notification.channel.NotificationChannelSender
import fanpoll.infra.notification.channel.email.EmailConfig
import fanpoll.infra.notification.channel.email.senders.SendGridSender
import fanpoll.infra.notification.channel.push.PushConfig
import fanpoll.infra.notification.channel.push.senders.FCMSender
import fanpoll.infra.notification.channel.push.token.*
import fanpoll.infra.notification.channel.sms.SMSConfig
import fanpoll.infra.notification.channel.sms.senders.MitakeSender
import fanpoll.infra.notification.channel.sms.senders.TwilioSMSSender
import fanpoll.infra.notification.logging.NotificationMessageLog
import fanpoll.infra.notification.logging.NotificationMessageLogDBWriter
import fanpoll.infra.notification.logging.NotificationMessageLogDTO
import fanpoll.infra.notification.logging.NotificationMessageLogTable
import fanpoll.infra.notification.message.DefaultNotificationMessageSender
import fanpoll.infra.notification.message.NotificationMessageBuilder
import fanpoll.infra.notification.message.NotificationMessageSender
import fanpoll.infra.notification.message.NotificationTemplateProcessor
import fanpoll.infra.notification.message.i18n.I18nProjectNotificationMessagesProvider
import fanpoll.infra.notification.senders.DefaultNotificationSender
import fanpoll.infra.notification.senders.NotificationCoroutineActor
import fanpoll.infra.notification.senders.NotificationSender
import io.ktor.server.application.Application
import org.koin.core.context.loadKoinModules
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ktor.ext.get

fun Application.loadNotificationModule(config: NotificationConfig, envMode: EnvMode) = loadKoinModules(module(createdAtStart = true) {
    val availableLangs = get<AvailableLangs>()
    val logWriter = get<LogWriter>()
    initNotificationMessageLogWriter(config)
    val notificationMessageBuilder = initNotificationMessageBuilder(config, availableLangs, envMode)
    val channelSenders = initChannelSenders(this, config, logWriter)
    initNotificationSender(config, availableLangs, logWriter, channelSenders, notificationMessageBuilder)
    registerResultRowMappers()
})


private fun Module.initNotificationSender(
    config: NotificationConfig,
    availableLangs: AvailableLangs,
    logWriter: LogWriter,
    channelSenders: Map<NotificationChannel, NotificationChannelSender>,
    notificationMessageBuilder: NotificationMessageBuilder
) {
    val defaultNotificationMessageSender = DefaultNotificationMessageSender(channelSenders)
    single<NotificationMessageSender> { defaultNotificationMessageSender }

    val defaultNotificationSender = DefaultNotificationSender(
        availableLangs, notificationMessageBuilder, defaultNotificationMessageSender, logWriter
    )
    val notificationSender = config.asyncExecutor?.let {
        NotificationCoroutineActor(it.coroutineActor, defaultNotificationSender)
    } ?: defaultNotificationSender
    single<NotificationSender> { defaultNotificationSender }

    KoinApplicationShutdownManager.register {
        notificationSender.shutdown()
    }
}

private fun Module.initNotificationMessageBuilder(
    config: NotificationConfig,
    availableLangs: AvailableLangs,
    envMode: EnvMode
): NotificationMessageBuilder {
    val i18NProjectNotificationMessagesProvider = I18nProjectNotificationMessagesProvider()
    single<I18nProjectNotificationMessagesProvider> { i18NProjectNotificationMessagesProvider }
    val templateProcessor = NotificationTemplateProcessor(availableLangs)
    single { templateProcessor }

    val notificationMessageBuilder = NotificationMessageBuilder(
        config.channels, envMode, availableLangs, i18NProjectNotificationMessagesProvider, templateProcessor
    )
    single { notificationMessageBuilder }

    return notificationMessageBuilder
}

private fun Application.initNotificationMessageLogWriter(config: NotificationConfig) {
    val logDestination = config.logging.destination
    val notificationMessageLogWriter = when (logDestination) {
        LogDestination.File -> get<FileLogWriter>()
        LogDestination.Database -> NotificationMessageLogDBWriter()
        LogDestination.Loki -> get<LokiLogWriter>()
        else -> throw InternalServerException(
            InfraResponseCode.SERVER_CONFIG_ERROR, "${logDestination} is invalid"
        )
    }
    val logEntityDispatcher = get<LogEntityDispatcher>()
    logEntityDispatcher.register(NotificationMessageLog.LOG_TYPE, notificationMessageLogWriter)
}

private fun initChannelSenders(
    module: Module,
    config: NotificationConfig,
    logWriter: LogWriter
): MutableMap<NotificationChannel, NotificationChannelSender> {
    val channelConfig = config.channels
    val defaultLoggingConfig = config.logging

    val channelSenders: MutableMap<NotificationChannel, NotificationChannelSender> = mutableMapOf()

    if (channelConfig.email != null) {
        channelConfig.email.logging = channelConfig.email.logging ?: defaultLoggingConfig
        channelSenders[NotificationChannel.Email] = initEmailSender(channelConfig.email, logWriter)
    }

    if (channelConfig.push != null) {
        channelConfig.push.logging = channelConfig.push.logging ?: defaultLoggingConfig
        channelSenders[NotificationChannel.Push] = initPushSender(module, channelConfig.push, logWriter)
    }

    if (channelConfig.sms != null) {
        channelConfig.sms.logging = channelConfig.sms.logging ?: defaultLoggingConfig
        channelSenders[NotificationChannel.SMS] = initSMSSender(channelConfig.sms, logWriter)
    }
    return channelSenders
}

private fun initEmailSender(emailConfig: EmailConfig, logWriter: LogWriter): NotificationChannelSender {
    val loggingConfig = emailConfig.logging!!
    return when {
        emailConfig.mock == true -> MockNotificationChannelSender(loggingConfig, logWriter)
        emailConfig.sendgrid != null -> SendGridSender(emailConfig.sendgrid, loggingConfig, logWriter)
        else -> throw InternalServerException(
            InfraResponseCode.SERVER_CONFIG_ERROR,
            "at least one email sender should be configured"
        )
    }
}

private fun initPushSender(module: Module, pushConfig: PushConfig, logWriter: LogWriter): NotificationChannelSender {
    val devicePushTokenRepository = DevicePushTokenExposedRepository(logWriter)
    module.single<DevicePushTokenRepository> { devicePushTokenRepository }

    val loggingConfig = pushConfig.logging!!
    return when {
        pushConfig.mock == true -> MockNotificationChannelSender(loggingConfig, logWriter)
        pushConfig.fcm != null -> FCMSender(pushConfig.fcm, devicePushTokenRepository, loggingConfig, logWriter)
        else -> throw InternalServerException(
            InfraResponseCode.SERVER_CONFIG_ERROR,
            "at least one push sender should be configured"
        )
    }
}

private fun initSMSSender(smsConfig: SMSConfig, logWriter: LogWriter): NotificationChannelSender {
    val loggingConfig = smsConfig.logging!!
    return when {
        smsConfig.mock == true -> MockNotificationChannelSender(loggingConfig, logWriter)
        smsConfig.twilio != null -> TwilioSMSSender(smsConfig.twilio, loggingConfig, logWriter)
        smsConfig.mitake != null -> MitakeSender(smsConfig.mitake, loggingConfig, logWriter)
        else -> throw InternalServerException(
            InfraResponseCode.SERVER_CONFIG_ERROR,
            "at least one sms sender should be configured"
        )
    }
}

private fun registerResultRowMappers() {
    ResultRowMappers.register(
        ResultRowMapper(DevicePushToken::class, DevicePushTokenTable),
        ResultRowMapper(DevicePushTokenDTO::class, DevicePushTokenTable),
        ResultRowMapper(NotificationMessageLogDTO::class, NotificationMessageLogTable)
    )
}