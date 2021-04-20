/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.kinesis

import fanpoll.infra.ServerConfig
import fanpoll.infra.logging.KinesisConfig
import fanpoll.infra.logging.LogMessage
import fanpoll.infra.logging.LogWriter
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mu.KotlinLogging
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.firehose.FirehoseAsyncClient
import software.amazon.awssdk.services.firehose.model.FirehoseException
import software.amazon.awssdk.services.firehose.model.PutRecordRequest
import software.amazon.awssdk.services.firehose.model.Record


class KinesisLogWriter(
    private val kinesisConfig: KinesisConfig,
    private val serverConfig: ServerConfig
) : LogWriter {

    private val logger = KotlinLogging.logger {}

    private val client: FirehoseAsyncClient

    init {
        val httpClientBuilder = NettyNioAsyncHttpClient.builder()
            .maxConcurrency(kinesisConfig.asyncHttpClient.maxConcurrency)
            .also {
                if (kinesisConfig.asyncHttpClient.maxPendingConnectionAcquires != null)
                    it.maxPendingConnectionAcquires(kinesisConfig.asyncHttpClient.maxPendingConnectionAcquires)
                if (kinesisConfig.asyncHttpClient.maxIdleConnectionTimeout != null)
                    it.connectionMaxIdleTime(kinesisConfig.asyncHttpClient.maxIdleConnectionTimeout)
            }
        client = FirehoseAsyncClient.builder().region(Region.AP_NORTHEAST_1).httpClientBuilder(httpClientBuilder).build()
    }

    override fun write(message: LogMessage) {
        val dataMap = mutableMapOf(
            "project" to JsonPrimitive(serverConfig.project),
            "env" to JsonPrimitive(serverConfig.env.name),
            "instance" to JsonPrimitive(serverConfig.instance),
            "dataType" to JsonPrimitive(message.type.name),
            "data" to message.content()
        )
        val dataJsonString = JsonObject(dataMap).toString()
        val streamName = if (message.type.isError()) kinesisConfig.errorStreamName else kinesisConfig.commonStreamName
        val putRecordRequest = PutRecordRequest.builder()
            .deliveryStreamName(streamName)
            .record(Record.builder().data(SdkBytes.fromUtf8String(dataJsonString)).build())
            .build()

        try {
            client.putRecord(putRecordRequest).toCompletableFuture().get()
        } catch (ex: Throwable) {
            if (ex is FirehoseException) {
                with(ex.awsErrorDetails()) {
                    logger.error(
                        "[KinesisLogger] send failure => logType = ${message.type.name}, data stream $streamName, " +
                                "requestId = ${ex.requestId()}, errorCode = ${errorCode()}, errorMsg = ${errorMessage()}, " +
                                "data = $dataJsonString", ex
                    )
                }
            } else {
                logger.error("[KinesisLogger] send error => data = $dataJsonString", ex)
            }
        }
    }
}