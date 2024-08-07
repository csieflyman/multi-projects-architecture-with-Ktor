/*
 * Copyright (c) 2022. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel.email.senders

import com.sendgrid.Method
import com.sendgrid.Request
import com.sendgrid.Response
import com.sendgrid.SendGrid
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Attachments
import com.sendgrid.helpers.mail.objects.Content
import com.sendgrid.helpers.mail.objects.Email
import fanpoll.infra.base.json.kotlinx.json
import fanpoll.infra.logging.writers.LogWriter
import fanpoll.infra.notification.NotificationLogConfig
import fanpoll.infra.notification.channel.NotificationChannelSender
import fanpoll.infra.notification.channel.email.EmailContent
import fanpoll.infra.notification.logging.NotificationMessageLog
import fanpoll.infra.notification.message.NotificationMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Duration
import java.time.Instant

data class SendGridConfig(
    val apiKey: String
)

class SendGridSender(
    config: SendGridConfig,
    private val loggingConfig: NotificationLogConfig,
    private val logWriter: LogWriter
) : NotificationChannelSender {

    private val logger = KotlinLogging.logger {}

    private val senderName = "SendGridSender"
    private val client: SendGrid

    override val maxReceiversPerRequest: Int = 1

    init {
        client = SendGrid(config.apiKey)
    }

    override fun shutdown() {
        logger.info { "shutdown $senderName..." }
        // do nothing
        logger.info { "shutdown $senderName completed" }
    }

    override suspend fun send(message: NotificationMessage) {
        message.receivers.map { message.copy(receivers = listOf(it)) }.forEach { sendEmail(it) }
    }

    private suspend fun sendEmail(emailMessage: NotificationMessage) {
        val log = emailMessage.toNotificationMessageLog()

        val request = Request()
        request.method = Method.POST
        request.endpoint = "mail/send"
        try {
            val mail = toSendGridMail(emailMessage)
            request.body = mail.build()
            log.sendAt = Instant.now()
        } catch (ex: Throwable) {
            log.success = false
            log.content = emailMessage.content.toJson().toString()
            log.errorMsg = "[$senderName] process email content error: ${ex.message}"
            logger.error(ex) { "${log.errorMsg} => ${json.encodeToJsonElement(NotificationMessageLog.serializer(), log)}" }
            writeLog(log)
            return
        }

        try {
            val response: Response = client.api(request)

            log.rspAt = Instant.now()
            log.duration = Duration.between(log.sendAt, log.rspAt)
            log.rspCode = response.statusCode.toString()
            log.rspBody = response.body
            //response.headers

            if (loggingConfig.logSuccess) {
                if (loggingConfig.logSuccessReqBody)
                    log.content = emailMessage.content.toJson().toString()
                writeLog(log)
            }
            logger.debug { "[$senderName] sendEmail: ${json.encodeToJsonElement(NotificationMessageLog.serializer(), log)}" }
        } catch (ex: Throwable) {
            log.rspAt = Instant.now()
            log.duration = Duration.between(log.sendAt, log.rspAt)
            log.success = false
            log.content = emailMessage.content.toJson().toString()
            log.errorMsg = "[$senderName] sendEmail error: ${ex.message}"
            logger.error(ex) { "${log.errorMsg} => ${json.encodeToJsonElement(NotificationMessageLog.serializer(), log)}" }
            writeLog(log)
        }
    }

    private suspend fun writeLog(notificationMessageLog: NotificationMessageLog) {
        if (loggingConfig.enabled)
            logWriter.write(notificationMessageLog)
    }

    private fun toSendGridMail(emailMessage: NotificationMessage): Mail {
        val myContent = emailMessage.content as EmailContent

        val from = Email(emailMessage.sender!!)
        val to = Email(emailMessage.receivers[0])
        val subject = myContent.subject
        val content = Content("text/html", myContent.body)

        return Mail(from, subject, to, content).apply {
            attachments = myContent.attachments?.map {
                Attachments.Builder(it.fileName, it.content.inputStream())
                    .withType(it.mimeType)
                    .withDisposition("attachment")
                    .build()
            }
        }
    }
}