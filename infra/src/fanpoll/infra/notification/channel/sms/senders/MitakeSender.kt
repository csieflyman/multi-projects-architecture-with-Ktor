/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel.sms.senders

import fanpoll.infra.logging.writers.LogWriter
import fanpoll.infra.notification.NotificationLogConfig
import fanpoll.infra.notification.NotificationMessage
import fanpoll.infra.notification.channel.NotificationChannelSender
import fanpoll.infra.notification.logging.NotificationMessageLog
import io.github.oshai.kotlinlogging.KotlinLogging

data class MitakeConfig(
    val port9600Url: String,
    val port7002Url: String,
    val user: String,
    val password: String,
    val callbackUrl: String? = null
)

class MitakeSender(
    private val config: MitakeConfig,
    private val loggingConfig: NotificationLogConfig,
    private val logWriter: LogWriter
) : NotificationChannelSender {

    private val logger = KotlinLogging.logger {}

    private val senderName = "MitakeSender"

    override val maxReceiversPerRequest: Int = 500

    init {

    }

    override fun send(message: NotificationMessage) {
        TODO("Not yet implemented")
    }

    private fun writeLog(notificationMessageLog: NotificationMessageLog) {
        if (loggingConfig.enabled)
            logWriter.write(notificationMessageLog)
    }

    override fun shutdown() {
        logger.info("shutdown $senderName...")
        logger.info("shutdown $senderName completed")
    }
}