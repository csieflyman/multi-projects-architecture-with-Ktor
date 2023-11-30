/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel.push.senders

import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.*
import fanpoll.infra.app.PushTokenStorage
import fanpoll.infra.base.async.ThreadPoolConfig
import fanpoll.infra.base.async.ThreadPoolUtils
import fanpoll.infra.base.json.json
import fanpoll.infra.logging.writers.LogWriter
import fanpoll.infra.notification.NotificationLogConfig
import fanpoll.infra.notification.NotificationMessage
import fanpoll.infra.notification.channel.NotificationChannelSender
import fanpoll.infra.notification.channel.push.PushContent
import fanpoll.infra.notification.logging.NotificationMessageLog
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ExecutorService

data class FCMConfig(
    val connectTimeout: Int = 60000,
    val readTimeout: Int = 180000,
    val threadPool: ThreadPoolConfig
) {

    class Builder {

        var connectTimeout: Int = 60000
        var readTimeout: Int = 180000
        private lateinit var threadPoolConfig: ThreadPoolConfig

        fun threadPool(block: ThreadPoolConfig.Builder.() -> Unit) {
            threadPoolConfig = ThreadPoolConfig.Builder().apply(block).build()
        }

        fun build(): FCMConfig {
            return FCMConfig(connectTimeout, readTimeout, threadPoolConfig)
        }
    }
}

class FCMSender(
    config: FCMConfig,
    private val pushTokenStorage: PushTokenStorage,
    private val loggingConfig: NotificationLogConfig,
    private val logWriter: LogWriter
) : NotificationChannelSender {

    private val logger = KotlinLogging.logger {}

    private val senderName = "FCMSender"
    private val executor: ExecutorService = ThreadPoolUtils.createThreadPoolExecutor(senderName, config.threadPool)
    private val client: FirebaseMessaging

    override val maxReceiversPerRequest: Int = 500

    init {
        val options = FirebaseOptions.builder().setCredentials(GoogleCredentials.getApplicationDefault())
            .setConnectTimeout(config.connectTimeout)
            .setReadTimeout(config.readTimeout).build()
        val app = FirebaseApp.initializeApp(options, senderName)
        client = FirebaseMessaging.getInstance(app)
    }

    override fun shutdown() {
        logger.info { "shutdown $senderName..." }
        executor.shutdown()
        logger.info { "shutdown $senderName completed" }
    }

    override fun send(message: NotificationMessage) {
        if (message.receivers.size == 1)
            sendSingle(message)
        else
            sendMulticast(message)
    }

    private fun sendSingle(message: NotificationMessage) {
        val token = message.receivers.first()
        val fcmMessage: Message = Message.builder()
            //.setNotification(buildFCMNotification(model))
            .putAllData(toFCMDataMap(message))
            .setToken(token)
            .build()

        val log = message.toNotificationMessageLog()
        log.sendAt = Instant.now()

        ApiFutures.addCallback(client.sendAsync(fcmMessage), object : ApiFutureCallback<String> {

            override fun onSuccess(messageId: String) {
                log.rspAt = Instant.now()
                log.duration = Duration.between(log.sendAt, log.rspAt)

                if (loggingConfig.logSuccess) {
                    if (loggingConfig.logSuccessReqBody) {
                        log.content = message.content.toJson().toString()
                    }
                    if (loggingConfig.logSuccessRspBody) {
                        log.successList = JsonArray(
                            listOf(json.encodeToJsonElement(FCMSuccessResponse.serializer(), FCMSuccessResponse(token, messageId)))
                        )
                    }
                    writeLog(log)
                }
                logger.debug { "[$senderName] sendSingle success: ${json.encodeToJsonElement(NotificationMessageLog.serializer(), log)}" }
            }

            override fun onFailure(ex: Throwable) {
                onFailure(message, log, ex)
            }
        }, executor)
    }

    private fun sendMulticast(message: NotificationMessage) {
        val fcmMessage: MulticastMessage = MulticastMessage.builder()
            //.setNotification(buildFCMNotification(model))
            .putAllData(toFCMDataMap(message))
            .addAllTokens(message.receivers)
            .build()

        val log = message.toNotificationMessageLog()
        log.sendAt = Instant.now()

        ApiFutures.addCallback(client.sendEachForMulticastAsync(fcmMessage), object : ApiFutureCallback<BatchResponse> {

            override fun onSuccess(response: BatchResponse) {
                log.rspAt = Instant.now()
                log.duration = Duration.between(log.sendAt, log.rspAt)

                val successResponses = mutableListOf<FCMSuccessResponse>()
                val failureResponses = mutableListOf<FCMFailureResponse>()
                val unRegisteredTokens = mutableListOf<String>()

                response.responses.forEachIndexed { index, sendResponse ->
                    if (sendResponse.isSuccessful) {
                        successResponses += FCMSuccessResponse(message.receivers[index], sendResponse.messageId)
                    } else {
                        with(sendResponse.exception) {
                            if (messagingErrorCode == MessagingErrorCode.UNREGISTERED) {
                                unRegisteredTokens += message.receivers[index]
                            } else {
                                failureResponses += FCMFailureResponse(
                                    message.receivers[index],
                                    messagingErrorCode.name,
                                    this.message,
                                    httpResponse?.content
                                )
                            }
                        }
                    }
                }

                if (unRegisteredTokens.isNotEmpty()) {
                    log.invalidRecipientIds = unRegisteredTokens
                    pushTokenStorage.deleteUnRegisteredTokens(unRegisteredTokens.toSet())
                }
                if (failureResponses.isNotEmpty()) {
                    log.success = false
                    log.failureList = JsonArray(
                        failureResponses.map { json.encodeToJsonElement(FCMFailureResponse.serializer(), it) }
                    )
                }

                if (log.success) {
                    if (loggingConfig.logSuccessReqBody)
                        log.content = message.content.toJson().toString()
                    if (!loggingConfig.logSuccessRspBody)
                        log.successList = JsonArray(
                            successResponses.map { json.encodeToJsonElement(FCMSuccessResponse.serializer(), it) }
                        )
                } else {
                    log.successList = JsonArray(
                        successResponses.map { json.encodeToJsonElement(FCMSuccessResponse.serializer(), it) }
                    )
                    log.content = message.content.toJson().toString()
                }

                if (!log.success || !log.invalidRecipientIds.isNullOrEmpty() || loggingConfig.logSuccess) {
                    writeLog(log)
                }
                logger.debug { "[$senderName] sendMulticast: ${json.encodeToJsonElement(NotificationMessageLog.serializer(), log)}" }
            }

            override fun onFailure(ex: Throwable) {
                onFailure(message, log, ex)
            }
        }, executor)
    }

    private fun onFailure(message: NotificationMessage, log: NotificationMessageLog, ex: Throwable) {
        log.rspAt = Instant.now()
        log.duration = Duration.between(log.sendAt, log.rspAt)

        if (ex is FirebaseMessagingException) {
            if (ex.messagingErrorCode == MessagingErrorCode.UNREGISTERED) {
                val tokens = message.receivers
                log.invalidRecipientIds = tokens
                pushTokenStorage.deleteUnRegisteredTokens(tokens.toSet())
            } else {
                log.rspCode = ex.messagingErrorCode.name
                log.rspMsg = ex.message
                log.rspBody = ex.httpResponse?.content
                log.success = false
                log.content = message.content.toJson().toString()
                log.errorMsg = "[$senderName] send error: ${ex.message}"
                logger.error(ex) { "${log.errorMsg} => ${json.encodeToJsonElement(NotificationMessageLog.serializer(), log)}" }
            }
        } else {
            log.rspMsg = ex.message
            log.success = false
            log.content = message.content.toJson().toString()
            log.errorMsg = "[$senderName] send unexpected error: ${ex.message}"
            logger.error(ex) { "${log.errorMsg} => ${json.encodeToJsonElement(NotificationMessageLog.serializer(), log)}" }
        }
        writeLog(log)
    }

    private fun writeLog(notificationMessageLog: NotificationMessageLog) {
        if (loggingConfig.enabled)
            logWriter.write(notificationMessageLog)
    }

    private fun toFCMDataMap(message: NotificationMessage): Map<String, String> {
        with(message) {
            val content = content as PushContent
            val map = mutableMapOf(
                "id" to id.toString(),
                "type" to type.name,
                "category" to type.category.name,
                "title" to content.title,
                "body" to content.body
            )
            version?.also { map["version"] = it }
            content.data?.forEach { (k, v) -> map[k] = v }
            return map.toMap()
        }
    }

    private fun buildFCMNotification(message: NotificationMessage): Notification {
        return with(message.content as PushContent) {
            Notification.builder().setTitle(title).setBody(body).build()
        }
    }

    @Serializable
    data class FCMSuccessResponse(
        val token: String,
        val messageId: String
    )

    @Serializable
    data class FCMFailureResponse(
        val token: String,
        val errorCode: String,
        val message: String? = null,
        val body: String? = null
    )

// Not Implemented yet
//    private fun sendTopic() {}
//
//    private fun sendBatch() {}
}