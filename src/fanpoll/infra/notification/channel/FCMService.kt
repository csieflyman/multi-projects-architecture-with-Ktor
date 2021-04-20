/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel

import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.*
import fanpoll.infra.EnvMode
import fanpoll.infra.InternalServerErrorException
import fanpoll.infra.ResponseCode
import fanpoll.infra.app.UserDeviceTable
import fanpoll.infra.database.myTransaction
import fanpoll.infra.logging.ErrorLogDTO
import fanpoll.infra.logging.LogManager
import fanpoll.infra.logging.LogMessage
import fanpoll.infra.logging.LogType
import fanpoll.infra.utils.DispatcherConfig
import fanpoll.infra.utils.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import mu.KotlinLogging
import org.jetbrains.exposed.sql.deleteWhere
import java.time.Instant
import java.util.concurrent.*

data class PushMessageConfig(
    val logSuccess: Boolean,
    val fcm: FCMConfig
)

data class FCMConfig(
    val connectTimeout: Int?,
    val readTimeout: Int,
    val executor: DispatcherConfig
)

object FCMService : NotificationChannelService {

    private val logger = KotlinLogging.logger {}

    private const val fcmAppName = "FCM"
    private const val fcmMaxTokensPerRequest = 500

    private lateinit var config: PushMessageConfig
    private lateinit var envMode: EnvMode

    private lateinit var executor: ExecutorService
    private lateinit var client: FirebaseMessaging

    fun init(config: PushMessageConfig, envMode: EnvMode) {
        FCMService.config = config
        FCMService.envMode = envMode

        val fcmConfig = config.fcm
        val factory = FCMThreadFactory()
        executor = with(fcmConfig.executor) {
            if (fixedPoolSize != null)
                Executors.newFixedThreadPool(fixedPoolSize, factory)
            else
                ThreadPoolExecutor(
                    minPoolSize!!, maxPoolSize!!,
                    keepAliveTime!!, TimeUnit.SECONDS,
                    LinkedBlockingQueue(), factory
                )
        }

        val options = FirebaseOptions.builder().setCredentials(GoogleCredentials.getApplicationDefault())
            .setConnectTimeout(fcmConfig.connectTimeout ?: 0)
            .setReadTimeout(fcmConfig.readTimeout).build()
        val app = FirebaseApp.initializeApp(options, fcmAppName)
        client = FirebaseMessaging.getInstance(app)
    }

    private class FCMThreadFactory : ThreadFactory {

        private val backingThreadFactory: ThreadFactory = Executors.defaultThreadFactory()

        override fun newThread(r: Runnable): Thread {
            val t = backingThreadFactory.newThread(r)
            t.name = "FCM-${t.name}"
            return t
        }
    }

    override fun shutdown() {
        logger.info("shutdown FCMService...")
        executor.shutdown()
        logger.info("shutdown FCMService completed")
    }

    fun send(pushMessage: PushMessage) {
        logger.debug { "push message: $pushMessage" }
        require(pushMessage.tokens.isNotEmpty())

        if (pushMessage.sendTime == null)
            pushMessage.sendTime = Instant.now()

        if (pushMessage.tokens.size == 1) {
            sendSingle(pushMessage)
        } else {
            sendMulticast(pushMessage)
        }
    }

    private fun sendSingle(pushMessage: PushMessage) {
        val token = pushMessage.tokens.first()
        val message: Message = Message.builder()
            //.setNotification(buildFCMNotification(model))
            .putAllData(pushMessage.toFCMDataMap(envMode))
            .setToken(token)
            .build()

        ApiFutures.addCallback(client.sendAsync(message), object : ApiFutureCallback<String> {

            override fun onSuccess(messageId: String) {
                if (config.logSuccess) {
                    val logDTO = pushMessage.toLogDTO()
                    logDTO.rspTime = Instant.now()
                    logDTO.successList = JsonArray(
                        listOf(json.encodeToJsonElement(FCMSuccessResponse.serializer(), FCMSuccessResponse(token, messageId)))
                    )
                    logger.debug("[FCM] sendSingle success: ${json.encodeToJsonElement(NotificationChannelLog.serializer(), logDTO)}")
                    LogManager.writeAsync(LogMessage(LogType.NOTIFICATION, logDTO))
                }
            }

            override fun onFailure(ex: Throwable) {
                onFailure(pushMessage, ex)
            }
        }, executor)
    }

    private fun sendMulticast(pushMessage: PushMessage) {
        val allTokens = pushMessage.tokens.toList()
        var start = 0
        while (start < allTokens.size) {
            val end = (start + fcmMaxTokensPerRequest).coerceAtMost(allTokens.size)
            val tokens = allTokens.subList(start, end)
            sendMulticast(tokens, pushMessage)
            start += fcmMaxTokensPerRequest
        }
    }

    private fun sendMulticast(tokens: List<String>, pushMessage: PushMessage) {
        val message: MulticastMessage = MulticastMessage.builder()
            //.setNotification(buildFCMNotification(model))
            .putAllData(pushMessage.toFCMDataMap(envMode))
            .addAllTokens(tokens)
            .build()

        ApiFutures.addCallback(client.sendMulticastAsync(message), object : ApiFutureCallback<BatchResponse> {

            override fun onSuccess(response: BatchResponse) {
                val logDTO = pushMessage.toLogDTO()
                logDTO.rspTime = Instant.now()

                val successResponses = mutableListOf<FCMSuccessResponse>()
                val failureResponses = mutableListOf<FCMFailureResponse>()
                val unRegisteredTokens = mutableListOf<String>()

                response.responses.forEachIndexed { index, sendResponse ->
                    if (sendResponse.isSuccessful) {
                        successResponses += FCMSuccessResponse(tokens[index], sendResponse.messageId)
                    } else {
                        with(sendResponse.exception) {
                            if (messagingErrorCode == MessagingErrorCode.UNREGISTERED) {
                                unRegisteredTokens += tokens[index]
                            } else {
                                failureResponses += FCMFailureResponse(
                                    tokens[index],
                                    messagingErrorCode.name,
                                    this.message,
                                    httpResponse.content
                                )
                            }
                        }
                    }
                }

                if (unRegisteredTokens.isNotEmpty()) {
                    logDTO.invalidRecipientIds = unRegisteredTokens
                    deleteUnRegisteredTokens(unRegisteredTokens.toSet())
                }
                if (successResponses.isNotEmpty()) {
                    logDTO.successList = JsonArray(
                        successResponses.map { json.encodeToJsonElement(FCMSuccessResponse.serializer(), it) }
                    )
                }
                if (failureResponses.isNotEmpty()) {
                    logDTO.rspCode = "FCM Batch Error"
                    logDTO.failureList = JsonArray(
                        failureResponses.map { json.encodeToJsonElement(FCMFailureResponse.serializer(), it) }
                    )
                }

                logger.debug("[FCM] sendMulticast: ${json.encodeToJsonElement(NotificationChannelLog.serializer(), logDTO)}")

                if (config.logSuccess && (successResponses.isNotEmpty() || unRegisteredTokens.isNotEmpty())) {
                    LogManager.writeAsync(LogMessage(LogType.NOTIFICATION, logDTO))
                }
                if (failureResponses.isNotEmpty()) {
                    LogManager.writeAsync(LogMessage(LogType.NOTIFICATION_ERROR, logDTO))
                }
            }

            override fun onFailure(ex: Throwable) {
                onFailure(pushMessage, ex)
            }
        }, executor)
    }

    private fun onFailure(pushMessage: PushMessage, ex: Throwable) {
        val tokens = pushMessage.tokens.toList()
        val logDTO = pushMessage.toLogDTO()
        logDTO.rspTime = Instant.now()

        if (ex is FirebaseMessagingException) {
            if (ex.messagingErrorCode == MessagingErrorCode.UNREGISTERED) {
                logDTO.invalidRecipientIds = tokens
                if (config.logSuccess) {
                    LogManager.writeAsync(LogMessage(LogType.NOTIFICATION, logDTO))
                }
                deleteUnRegisteredTokens(tokens.toSet())
            } else {
                logDTO.rspCode = ex.messagingErrorCode.name
                logDTO.rspMsg = ex.message
                logDTO.rspBody = ex.httpResponse.content
                LogManager.writeAsync(LogMessage(LogType.NOTIFICATION_ERROR, logDTO))
            }
            logger.error("[FCM] send failure: ${json.encodeToJsonElement(NotificationChannelLog.serializer(), logDTO)}", ex)
        } else {
            logDTO.rspCode = "[FCM] Undefined Error"
            logDTO.rspMsg = ex.message
            logger.error("[FCM] send error: ${json.encodeToJsonElement(NotificationChannelLog.serializer(), logDTO)}", ex)
            LogManager.writeAsync(LogMessage(LogType.NOTIFICATION_ERROR, logDTO))
        }
    }

    private fun deleteUnRegisteredTokens(tokens: Collection<String>) {
        try {
            myTransaction {
                UserDeviceTable.deleteWhere { UserDeviceTable.pushToken inList tokens.toSet() }
            }
        } catch (e: Throwable) {
            logger.error("[FCM] fail to deleteUnRegisteredTokens: $tokens", e)
            LogManager.writeAsync(
                LogMessage(
                    LogType.SERVER_ERROR, ErrorLogDTO.internal(
                        InternalServerErrorException(ResponseCode.NOTIFICATION_ERROR, null, e),
                        fcmAppName, "deleteUnRegisteredTokens"
                    )
                )
            )
        }
    }

    private fun buildFCMNotification(pushMessage: PushMessage): com.google.firebase.messaging.Notification {
        return with(pushMessage.content) {
            com.google.firebase.messaging.Notification.builder()
                .setTitle(title).setBody(body).build()
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