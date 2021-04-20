/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel

import com.google.common.base.Joiner
import fanpoll.infra.EnvMode
import fanpoll.infra.logging.AwsAsyncHttpClientConfig
import fanpoll.infra.logging.LogManager
import fanpoll.infra.logging.LogMessage
import fanpoll.infra.logging.LogType
import fanpoll.infra.notification.NotificationPurpose
import mu.KotlinLogging
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ses.SesAsyncClient
import software.amazon.awssdk.services.ses.model.RawMessage
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest
import software.amazon.awssdk.services.ses.model.SesException
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.time.Instant
import java.util.*
import javax.activation.DataHandler
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource

data class EmailConfig(
    val logSuccess: Boolean,
    val noReplyAddress: String,
    val marketingAddress: String? = null,
    val sendMeTooAddresses: Set<String>? = null,
    val ses: SESConfig
)

data class SESConfig(
    val asyncHttpClient: AwsAsyncHttpClientConfig
)

object SESService : NotificationChannelService {

    private val logger = KotlinLogging.logger {}

    private lateinit var config: EmailConfig
    private lateinit var envMode: EnvMode

    private lateinit var client: SesAsyncClient

    fun init(config: EmailConfig, envMode: EnvMode) {
        SESService.config = config
        SESService.envMode = envMode

        val sesConfig = config.ses
        val httpClientBuilder = NettyNioAsyncHttpClient.builder()
            .maxConcurrency(sesConfig.asyncHttpClient.maxConcurrency)
            .also {
                if (sesConfig.asyncHttpClient.maxPendingConnectionAcquires != null)
                    it.maxPendingConnectionAcquires(sesConfig.asyncHttpClient.maxPendingConnectionAcquires)
                if (sesConfig.asyncHttpClient.maxIdleConnectionTimeout != null)
                    it.connectionMaxIdleTime(sesConfig.asyncHttpClient.maxIdleConnectionTimeout)
            }
        client = SesAsyncClient.builder().region(Region.US_EAST_1).httpClientBuilder(httpClientBuilder).build()
    }

    override fun shutdown() {
        logger.info("shutdown SESService...")
        client.close()
        logger.info("shutdown SESService completed")
    }

    fun send(emailMessage: EmailMessage) {
        logger.debug { "send email: $emailMessage" }
        require(emailMessage.to.isNotEmpty())

        emailMessage.sender = when (emailMessage.type.purpose) {
            NotificationPurpose.System -> config.noReplyAddress
            NotificationPurpose.Marketing -> config.marketingAddress ?: config.noReplyAddress
        }
        if (emailMessage.sendTime == null)
            emailMessage.sendTime = Instant.now()

        if (emailMessage.type.broadcast) {
            emailMessage.to.map { emailMessage.copy(to = setOf(it)) }.forEach {
                sendToSES(it)
            }
        } else {
            sendToSES(emailMessage) // maximum recipients = 50
        }

        if (emailMessage.type.sendMeToo) {
            sendToSES(emailMessage.copy(to = config.sendMeTooAddresses!!))
        }
    }

    private fun sendToSES(emailMessage: EmailMessage) {
        val bytes = buildJavaMailMessage(emailMessage)
        val data = SdkBytes.fromByteArray(bytes) // maximum size = 10MB
        val rawMessage = RawMessage.builder().data(data).build()
        val rawEmailRequest: SendRawEmailRequest = SendRawEmailRequest.builder().rawMessage(rawMessage).build()

        try {
            client.sendRawEmail(rawEmailRequest).thenAccept { response ->
                if (config.logSuccess) {
                    val logDTO = emailMessage.toLogDTO()
                    logDTO.rspTime = Instant.now()
                    logDTO.rspMsg = response.messageId()
                    LogManager.writeAsync(LogMessage(LogType.NOTIFICATION, logDTO))
                }
            }.get()

        } catch (ex: Throwable) {
            val logDTO = emailMessage.toLogDTO()
            logDTO.rspTime = Instant.now()
            if (ex is SesException) {
                with(ex.awsErrorDetails()) {
                    logger.error(
                        "[SES] send failure => requestId = ${ex.requestId()}, " +
                                "errorCode = ${errorCode()}, errorMsg = ${errorMessage()}" +
                                "email = $emailMessage", ex
                    )
                    logDTO.rspCode = errorCode()
                    logDTO.rspMsg = errorMessage()
                    logDTO.rspBody = ex.requestId()
                }
            } else {
                logger.error("[SES] send error => email = $emailMessage", ex)
                logDTO.rspCode = "[SES] Unexpected Error"
                logDTO.rspMsg = ex.message
            }
            LogManager.writeAsync(LogMessage(LogType.NOTIFICATION_ERROR, logDTO))
        }
    }

    private fun buildJavaMailMessage(emailMessage: EmailMessage): ByteArray {
        val session = Session.getDefaultInstance(Properties())
        val javaMailMessage = MimeMessage(session)

        val subject = if (envMode != EnvMode.prod) "[${envMode.name}] " + emailMessage.content.subject
        else emailMessage.content.subject

        javaMailMessage.setSubject(subject, "UTF-8")
        javaMailMessage.setFrom(InternetAddress(emailMessage.sender!!))
        javaMailMessage.setRecipients(
            javax.mail.Message.RecipientType.TO,
            InternetAddress.parse(Joiner.on(",").join(emailMessage.to))
        )

        val multipart = MimeMultipart()

        // html body
        val htmlBodyPart = MimeBodyPart()
        htmlBodyPart.setContent(emailMessage.content.body, "text/html; charset=UTF-8")
        multipart.addBodyPart(htmlBodyPart)

        // attachment
        emailMessage.content.attachments?.forEach {
            val attachmentPart = MimeBodyPart()
            val fds = ByteArrayDataSource(it.content, it.mimeType.value)
            fds.name = it.fileName
            attachmentPart.dataHandler = DataHandler(fds)
            attachmentPart.fileName = fds.name
            multipart.addBodyPart(attachmentPart)
        }

        javaMailMessage.setContent(multipart)
        val outputStream = ByteArrayOutputStream()
        javaMailMessage.writeTo(outputStream)
        val buf = ByteBuffer.wrap(outputStream.toByteArray())
        val arr = ByteArray(buf.remaining())
        buf[arr]
        return arr
    }
}