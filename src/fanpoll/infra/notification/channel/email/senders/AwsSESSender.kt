/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel.email.senders

import fanpoll.infra.base.aws.NettyHttpClientConfig
import fanpoll.infra.base.aws.configure
import fanpoll.infra.base.json.json
import fanpoll.infra.logging.writers.LogWriter
import fanpoll.infra.notification.NotificationLogConfig
import fanpoll.infra.notification.NotificationMessage
import fanpoll.infra.notification.channel.NotificationChannelSender
import fanpoll.infra.notification.channel.email.EmailContent
import fanpoll.infra.notification.channel.email.EmailUtils
import fanpoll.infra.notification.logging.NotificationMessageLog
import mu.KotlinLogging
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ses.SesAsyncClient
import software.amazon.awssdk.services.ses.model.RawMessage
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest
import software.amazon.awssdk.services.ses.model.SesException
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.time.Duration
import java.time.Instant
import java.util.*
import javax.mail.Session
import javax.mail.internet.MimeMessage

data class AwsSESConfig(
    val nettyHttpClient: NettyHttpClientConfig? = null
) {

    class Builder {

        private var nettyHttpClientConfig: NettyHttpClientConfig? = null

        fun nettyHttpClient(block: NettyHttpClientConfig.Builder.() -> Unit) {
            nettyHttpClientConfig = NettyHttpClientConfig.Builder().apply(block).build()
        }

        fun build(): AwsSESConfig {
            return AwsSESConfig(nettyHttpClientConfig)
        }
    }
}

class AwsSESSender(
    config: AwsSESConfig,
    private val loggingConfig: NotificationLogConfig,
    private val logWriter: LogWriter
) : NotificationChannelSender {

    private val logger = KotlinLogging.logger {}

    private val senderName = "AwsSESSender"
    private val client: SesAsyncClient

    override val maxReceiversPerRequest: Int = 1 // SendBulkTemplatedEmail => 50

    init {
        val sesAsyncClientBuilder = SesAsyncClient.builder().region(Region.US_EAST_1)
        if (config.nettyHttpClient != null) {
            sesAsyncClientBuilder.configure(senderName, config.nettyHttpClient)
        }
        client = sesAsyncClientBuilder.build()
    }

    override fun shutdown() {
        logger.info("shutdown $senderName...")
        client.close()
        logger.info("shutdown $senderName completed")
    }

    override fun send(message: NotificationMessage) {
        message.receivers.map { message.copy(receivers = listOf(it)) }.forEach { sendEmail(it) }
    }

    private fun sendEmail(emailMessage: NotificationMessage) {
        val log = emailMessage.toNotificationMessageLog()

        val rawMessage = try {
            toRawMessage(toMimeMessage(emailMessage))
        } catch (ex: Throwable) {
            log.success = false
            log.content = emailMessage.content.toJson().toString()
            log.errorMsg = "[$senderName] process message error: ${ex.message}"
            logger.error("${log.errorMsg} => ${json.encodeToJsonElement(NotificationMessageLog.serializer(), log)}", ex)
            writeLog(log)
            return
        }

        val rawEmailRequest: SendRawEmailRequest = SendRawEmailRequest.builder().rawMessage(rawMessage).build()
        log.sendAt = Instant.now()
        client.sendRawEmail(rawEmailRequest).whenComplete { response, ex ->
            log.rspAt = Instant.now()
            log.rspTime = Duration.between(log.sendAt, log.rspAt).toMillis()

            if (response != null) {
                log.rspMsg = response.messageId()
                if (loggingConfig.logSuccess) {
                    if (loggingConfig.logSuccessReqBody)
                        log.content = emailMessage.content.toJson().toString()
                    writeLog(log)
                }
                logger.debug("[$senderName] sendRawEmail success: ${json.encodeToJsonElement(NotificationMessageLog.serializer(), log)}")
            } else {
                log.success = false
                log.content = emailMessage.content.toJson().toString()

                if (ex is SesException) {
                    with(ex.awsErrorDetails()) {
                        log.rspCode = errorCode()
                        log.rspMsg = errorMessage()
                        log.rspBody = ex.requestId()
                        log.errorMsg = "[$senderName] sendRawEmail error: ${ex.message}"
                    }
                } else {
                    log.errorMsg = "[$senderName] sendRawEmail unexpected error: ${ex.message}"
                }
                logger.error("${log.errorMsg} => ${json.encodeToJsonElement(NotificationMessageLog.serializer(), log)}", ex)
                writeLog(log)
            }
        }
    }

    private fun writeLog(notificationMessageLog: NotificationMessageLog) {
        if (loggingConfig.enabled)
            logWriter.write(notificationMessageLog)
    }

    private fun toMimeMessage(emailMessage: NotificationMessage): MimeMessage {
        val session = Session.getDefaultInstance(Properties())
        val mimeMessage = MimeMessage(session)
        EmailUtils.populateMimeMessage(
            mimeMessage, emailMessage.content as EmailContent,
            emailMessage.sender!!, emailMessage.receivers
        )
        return mimeMessage
    }

    private fun toRawMessage(mimeMessage: MimeMessage): RawMessage {
        val outputStream = ByteArrayOutputStream()
        mimeMessage.writeTo(outputStream)
        val bytes = outputStream.toByteArray()
        val data = SdkBytes.fromByteBuffer(ByteBuffer.wrap(bytes))// maximum size = 10MB
        return RawMessage.builder().data(data).build()
    }
}