/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.writers

import fanpoll.infra.AppInfoConfig
import fanpoll.infra.ServerConfig
import fanpoll.infra.base.aws.NettyHttpClientConfig
import fanpoll.infra.base.aws.configure
import fanpoll.infra.base.util.DateTimeUtils
import fanpoll.infra.logging.LogMessage
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mu.KotlinLogging
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.firehose.FirehoseAsyncClient
import software.amazon.awssdk.services.firehose.model.FirehoseException
import software.amazon.awssdk.services.firehose.model.PutRecordRequest
import software.amazon.awssdk.services.firehose.model.Record

data class AwsKinesisConfig(
    val streamName: String,
    val nettyHttpClient: NettyHttpClientConfig? = null
) {

    class Builder {

        lateinit var streamName: String
        private var nettyHttpClientConfig: NettyHttpClientConfig? = null

        fun nettyHttpClient(block: NettyHttpClientConfig.Builder.() -> Unit) {
            nettyHttpClientConfig = NettyHttpClientConfig.Builder().apply(block).build()
        }

        fun build(): AwsKinesisConfig {
            return AwsKinesisConfig(streamName, nettyHttpClientConfig)
        }
    }
}

class AwsKinesisLogWriter(
    private val awsKinesisConfig: AwsKinesisConfig,
    private val appInfoConfig: AppInfoConfig,
    private val serverConfig: ServerConfig
) : LogWriter {

    private val logger = KotlinLogging.logger {}

    private val writerName = "AwsKinesisLogWriter"
    private val client: FirehoseAsyncClient

    init {
        val firehoseClientBuilder = FirehoseAsyncClient.builder().region(Region.AP_NORTHEAST_1)
        if (awsKinesisConfig.nettyHttpClient != null)
            firehoseClientBuilder.configure(writerName, awsKinesisConfig.nettyHttpClient)
        client = firehoseClientBuilder.build()
    }

    override fun shutdown() {
        logger.info("shutdown $writerName...")
        client.close()
        logger.info("shutdown $writerName completed")
    }

    override fun write(message: LogMessage) {
        val dataMap = mutableMapOf(
            "project" to JsonPrimitive(appInfoConfig.project),
            "tagVersion" to JsonPrimitive(appInfoConfig.git.tag),
            "env" to JsonPrimitive(serverConfig.env.name),
            "instanceId" to JsonPrimitive(serverConfig.instanceId),
            "logType" to JsonPrimitive(message.logType),
            "logLevel" to JsonPrimitive(message.logLevel.name),
            "occurAt" to JsonPrimitive(DateTimeUtils.UTC_DATE_TIME_PATTERN.format(message.occurAt)),
            "data" to message.toJson()
        )
        val dataJsonString = JsonObject(dataMap).toString()
        val putRecordRequest = PutRecordRequest.builder()
            .deliveryStreamName(awsKinesisConfig.streamName)
            .record(Record.builder().data(SdkBytes.fromUtf8String(dataJsonString)).build())
            .build()

        client.putRecord(putRecordRequest).exceptionally { ex ->
            if (ex is FirehoseException) {
                with(ex.awsErrorDetails()) {
                    logger.error(
                        "[$writerName] putRecord error => logType = ${message.logType}, logLevel = ${message.logLevel}" +
                                "requestId = ${ex.requestId()}, errorCode = ${errorCode()}, errorMsg = ${errorMessage()}, " +
                                "data = $dataJsonString", ex
                    )
                }
            } else {
                logger.error("[$writerName] putRecord unexpect error => data = $dataJsonString", ex)
            }
            null
        }
    }
}