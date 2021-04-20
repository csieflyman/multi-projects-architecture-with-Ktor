/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification

import fanpoll.infra.EnvMode
import fanpoll.infra.InternalServerErrorException
import fanpoll.infra.ResponseCode
import fanpoll.infra.notification.channel.*
import fanpoll.infra.utils.CoroutineUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import mu.KotlinLogging

object NotificationSender {

    private val logger = KotlinLogging.logger {}

    private const val dispatcherName = "Notification"
    private lateinit var dispatcher: CoroutineDispatcher
    private lateinit var channel: SendChannel<Notification>
    private lateinit var coroutineScope: CoroutineScope

    private lateinit var envMode: EnvMode

    private val notificationChannelServices: MutableMap<NotificationChannel, NotificationChannelService> = mutableMapOf()

    fun init(notificationConfig: NotificationConfig, envMode: EnvMode) {
        logger.info("========== init NotificationService... ==========")
        NotificationSender.envMode = envMode

        try {
            val channelConfig = notificationConfig.channels
            if (channelConfig.email != null) {
                logger.info("===== init Email-SESService... =====")
                SESService.init(channelConfig.email, envMode)
                notificationChannelServices[NotificationChannel.Email] = SESService
                logger.info("===== init Email-SESService completed =====")
            }
            if (channelConfig.push != null) {
                logger.info("===== init Push-FCMService... =====")
                FCMService.init(channelConfig.push, envMode)
                notificationChannelServices[NotificationChannel.Push] = FCMService
                logger.info("===== init Push-FCMService completed =====")
            }
            if (channelConfig.sms != null) {
                logger.info("===== init SMS-MitakeService... =====")
                MitakeService.init(channelConfig.sms)
                notificationChannelServices[NotificationChannel.SMS] = MitakeService
                logger.info("===== init SMS-MitakeService completed =====")
            }

            dispatcher = if (channelConfig.coroutine.dispatcher != null)
                CoroutineUtils.createDispatcher(dispatcherName, channelConfig.coroutine.dispatcher)
            else Dispatchers.IO

            val context = dispatcher // TODO coroutine exception handling
            coroutineScope = CoroutineScope(context)
            channel = CoroutineUtils.createActor(
                dispatcherName, channelConfig.coroutine.coroutines,
                coroutineScope, NotificationSender::receive
            )
        } catch (e: Throwable) {
            throw InternalServerErrorException(ResponseCode.NOTIFICATION_ERROR, "fail to init NotificationService", e)
        }
        logger.info("========== init NotificationService completed ==========")
    }

    fun shutdown() {
        logger.info("shutdown NotificationService...")
        closeCoroutine()
        notificationChannelServices.values.forEach { it.shutdown() }
        logger.info("shutdown NotificationService completed")
    }

    private fun closeCoroutine() {
        CoroutineUtils.closeChannel(dispatcherName, channel)
        coroutineScope.cancel(dispatcherName)
        if (dispatcher is ExecutorCoroutineDispatcher) {
            CoroutineUtils.closeDispatcher(dispatcherName, dispatcher as ExecutorCoroutineDispatcher)
        }
    }

    suspend fun sendAsync(message: Notification) {
        channel.send(message)
    }

    fun send(message: Notification) {
        runBlocking {
            channel.send(message)
        }
    }

    private fun receive(message: Notification) {
        when (message) {
            is NotificationChannelMessage -> sendChannelMessage(message)
            is NotificationCmdMessage -> sendChannelMessage(message.buildChannelMessages())
            is NotificationRemoteCmdMessage -> TODO() // put message into message queue then remote notification service send it
        }
    }

    private fun sendChannelMessage(message: NotificationChannelMessage) {
        logger.debug { "sendChannelMessage: ${message.type.id} - ${message.id}" }
        when (message) {
            is EmailMessage -> SESService.send(message)
            is PushMessage -> FCMService.send(message)
            is SMSMessage -> TODO()
            is MultiChannelsMessage -> {
                message.channels.forEach {
                    when (it) {
                        NotificationChannel.Email -> SESService.send(message.toEmailMessage())
                        NotificationChannel.Push -> FCMService.send(message.toPushMessage())
                        NotificationChannel.SMS -> TODO()
                    }
                }
            }
        }
    }
}