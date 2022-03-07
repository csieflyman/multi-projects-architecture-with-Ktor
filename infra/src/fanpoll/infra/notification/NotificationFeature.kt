/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification

import fanpoll.infra.MyApplicationConfig
import fanpoll.infra.app.PushTokenStorage
import fanpoll.infra.base.async.AsyncExecutorConfig
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.i18n.AvailableLangs
import fanpoll.infra.base.koin.KoinApplicationShutdownManager
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.logging.LogDestination
import fanpoll.infra.logging.request.LokiLogWriter
import fanpoll.infra.logging.writers.FileLogWriter
import fanpoll.infra.logging.writers.LogMessageDispatcher
import fanpoll.infra.logging.writers.LogWriter
import fanpoll.infra.notification.channel.MockNotificationChannelSender
import fanpoll.infra.notification.channel.NotificationChannelConfig
import fanpoll.infra.notification.channel.NotificationChannelSender
import fanpoll.infra.notification.channel.email.senders.SendGridSender
import fanpoll.infra.notification.channel.push.senders.FCMSender
import fanpoll.infra.notification.channel.sms.senders.MitakeSender
import fanpoll.infra.notification.i18n.I18nNotificationProjectMessages
import fanpoll.infra.notification.logging.NotificationMessageLog
import fanpoll.infra.notification.logging.NotificationMessageLogDBWriter
import fanpoll.infra.notification.senders.NotificationCoroutineActor
import fanpoll.infra.notification.senders.NotificationDispatcher
import io.ktor.application.Application
import io.ktor.application.ApplicationFeature
import io.ktor.util.AttributeKey
import mu.KotlinLogging
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.ext.koin

class NotificationFeature(configuration: Configuration) {

    class Configuration {

        private lateinit var channels: NotificationChannelConfig
        private lateinit var logging: NotificationLogConfig
        private var asyncExecutor: AsyncExecutorConfig? = null

        fun channels(block: NotificationChannelConfig.Builder.() -> Unit) {
            channels = NotificationChannelConfig.Builder().apply(block).build()
        }

        fun logging(configure: NotificationLogConfig.Builder.() -> Unit) {
            logging = NotificationLogConfig.Builder().apply(configure).build()
        }

        fun asyncExecutor(configure: AsyncExecutorConfig.Builder.() -> Unit) {
            asyncExecutor = AsyncExecutorConfig.Builder().apply(configure).build()
        }

        fun build(): NotificationConfig {
            return NotificationConfig(channels, logging, asyncExecutor)
        }
    }

    companion object Feature : ApplicationFeature<Application, Configuration, NotificationFeature> {

        override val key = AttributeKey<NotificationFeature>("Notification")

        private val logger = KotlinLogging.logger {}

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): NotificationFeature {
            val configuration = Configuration().apply(configure)
            val feature = NotificationFeature(configuration)

            val appConfig = pipeline.get<MyApplicationConfig>()
            val envMode = appConfig.server.env
            val config = appConfig.infra.notification ?: configuration.build()
            val channelConfig = config.channels
            val defaultLoggingConfig = config.logging

            pipeline.koin {
                modules(
                    module(createdAtStart = true) {

                        val availableLangs = pipeline.get<AvailableLangs>()
                        val i18nNotificationProjectMessagesProviders = I18nNotificationProjectMessages()
                        single { i18nNotificationProjectMessagesProviders }

                        val notificationMessageLogWriter = when (defaultLoggingConfig.destination) {
                            LogDestination.File -> pipeline.get<FileLogWriter>()
                            LogDestination.Database -> NotificationMessageLogDBWriter()
                            LogDestination.Loki -> pipeline.get<LokiLogWriter>()
                            else -> throw InternalServerException(
                                InfraResponseCode.SERVER_CONFIG_ERROR, "${defaultLoggingConfig.destination} is invalid"
                            )
                        }
                        val logMessageDispatcher = pipeline.get<LogMessageDispatcher>()
                        logMessageDispatcher.register(NotificationMessageLog.LOG_TYPE, notificationMessageLogWriter)
                        val logWriter = pipeline.get<LogWriter>()

                        var emailSender: NotificationChannelSender? = null
                        if (channelConfig.email != null) {
                            val loggingConfig = channelConfig.email.logging ?: defaultLoggingConfig
                            emailSender = when {
                                channelConfig.email.mock == true -> MockNotificationChannelSender(loggingConfig, logWriter)
                                channelConfig.email.sendgrid != null -> SendGridSender(
                                    channelConfig.email.sendgrid, loggingConfig, logWriter
                                )
                                else -> throw InternalServerException(
                                    InfraResponseCode.SERVER_CONFIG_ERROR,
                                    "at least one email sender should be configured"
                                )
                            }
                        }

                        var pushSender: NotificationChannelSender? = null
                        if (channelConfig.push != null) {
                            val loggingConfig = channelConfig.push.logging ?: defaultLoggingConfig
                            val pushTokenStorage = pipeline.get<PushTokenStorage>()
                            pushSender = when {
                                channelConfig.push.mock == true -> MockNotificationChannelSender(loggingConfig, logWriter)
                                channelConfig.push.fcm != null -> FCMSender(
                                    channelConfig.push.fcm,
                                    pushTokenStorage,
                                    loggingConfig,
                                    logWriter
                                )
                                else -> throw InternalServerException(
                                    InfraResponseCode.SERVER_CONFIG_ERROR,
                                    "at least one push sender should be configured"
                                )
                            }
                        }

                        var smsSender: NotificationChannelSender? = null
                        if (channelConfig.sms != null) {
                            val loggingConfig = channelConfig.sms.logging ?: defaultLoggingConfig
                            smsSender = when {
                                channelConfig.sms.mock == true -> MockNotificationChannelSender(loggingConfig, logWriter)
                                channelConfig.sms.mitake != null -> MitakeSender(channelConfig.sms.mitake, loggingConfig, logWriter)
                                else -> throw InternalServerException(
                                    InfraResponseCode.SERVER_CONFIG_ERROR,
                                    "at least one sms sender should be configured"
                                )
                            }
                        }

                        val notificationDispatcher = NotificationDispatcher(
                            channelConfig,
                            envMode,
                            availableLangs,
                            i18nNotificationProjectMessagesProviders,
                            emailSender,
                            pushSender,
                            smsSender
                        )

                        val notificationSender = config.asyncExecutor?.let {
                            NotificationCoroutineActor(it.coroutineActor, notificationDispatcher, logWriter)
                        } ?: notificationDispatcher

                        single { notificationSender }

                        KoinApplicationShutdownManager.register { notificationSender.shutdown() }
                    }
                )
            }

            return feature
        }
    }
}

data class NotificationConfig(
    val channels: NotificationChannelConfig,
    val logging: NotificationLogConfig,
    val asyncExecutor: AsyncExecutorConfig? = null,
)

data class NotificationLogConfig(
    val enabled: Boolean = true,
    val destination: LogDestination = LogDestination.File,
    val logSuccess: Boolean = false,
    val logSuccessReqBody: Boolean = false,
    val logSuccessRspBody: Boolean = false
) {

    class Builder {
        var enabled: Boolean = true
        var destination: LogDestination = LogDestination.File
        var logSuccess: Boolean = false
        var logSuccessReqBody: Boolean = false
        var logSuccessRspBody: Boolean = false

        fun build(): NotificationLogConfig {
            return NotificationLogConfig(enabled, destination, logSuccess, logSuccessReqBody, logSuccessRspBody)
        }
    }
}