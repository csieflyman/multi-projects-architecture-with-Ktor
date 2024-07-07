/*
 * Copyright (c) 2023. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel.sms.senders

import com.twilio.Twilio
import com.twilio.exception.ApiException
import com.twilio.rest.api.v2010.account.Message
import com.twilio.type.PhoneNumber
import fanpoll.infra.base.json.kotlinx.json
import fanpoll.infra.logging.writers.LogWriter
import fanpoll.infra.notification.NotificationLogConfig
import fanpoll.infra.notification.channel.NotificationChannelSender
import fanpoll.infra.notification.channel.sms.SMSContent
import fanpoll.infra.notification.logging.NotificationMessageLog
import fanpoll.infra.notification.message.NotificationMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Duration
import java.time.Instant

data class TwilioSMSConfig(
    val accountSid: String,
    val authToken: String,
    val fromPhoneNumber: String
)

class TwilioSMSSender(
    private val config: TwilioSMSConfig,
    private val loggingConfig: NotificationLogConfig,
    private val logWriter: LogWriter
) : NotificationChannelSender {

    private val logger = KotlinLogging.logger {}

    private val senderName = "TwilioSMSSender"

    override val maxReceiversPerRequest: Int = 1

    init {
        Twilio.init(config.accountSid, config.authToken)
    }

    override suspend fun send(message: NotificationMessage) {
        message.receivers.map { message.copy(receivers = listOf(it)) }.forEach { sendSingle(it) }
    }

    private suspend fun sendSingle(message: NotificationMessage) {
        val log = message.toNotificationMessageLog()
        val to = message.receivers[0]
        val body = (message.content as SMSContent).body

        try {
            log.sendAt = Instant.now()
            val twilioMessage = Message.creator(
                PhoneNumber(to),
                PhoneNumber(config.fromPhoneNumber),
                body
            )
            val response = twilioMessage.create()

            log.rspAt = Instant.now()
            log.duration = Duration.between(log.sendAt, log.rspAt)
            log.rspMsg = response.sid
            log.rspBody = response.body

            if (response.errorCode != null) {
                log.success = false
                log.rspCode = response.errorCode?.toString()
                log.errorMsg = response.errorMessage
            } else {
                log.success = true
                log.rspCode = response.status.name
            }

            if (loggingConfig.logSuccessReqBody)
                log.content = body
            if (!log.success || loggingConfig.logSuccess) {
                writeLog(log)
            }
            logger.debug { "[$senderName] sendSMS: ${json.encodeToJsonElement(NotificationMessageLog.serializer(), log)}" }
        } catch (ex: ApiException) {
            log.rspAt = Instant.now()
            log.duration = Duration.between(log.sendAt, log.rspAt)
            log.success = false
            log.rspCode = "statusCode = {${ex.statusCode}, errorCode = ${ex.code}"
            log.rspBody = ex.moreInfo
            log.errorMsg = "[$senderName] sendSMS error: ${ex.message}"
            logger.error(ex) { "${log.errorMsg} => ${json.encodeToJsonElement(NotificationMessageLog.serializer(), log)}" }
            writeLog(log)
        }
    }

    private suspend fun writeLog(notificationMessageLog: NotificationMessageLog) {
        if (loggingConfig.enabled)
            logWriter.write(notificationMessageLog)
    }

    override fun shutdown() {
        logger.info { "shutdown $senderName..." }
        Twilio.destroy()
        logger.info { "shutdown $senderName completed" }
    }
}