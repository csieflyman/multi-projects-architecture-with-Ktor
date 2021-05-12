/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

@file:OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)

package fanpoll.infra

import fanpoll.infra.controller.Form
import fanpoll.infra.utils.*
import io.ktor.application.ApplicationCall
import io.ktor.features.callId
import io.ktor.response.respond
import kotlinx.serialization.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import java.util.*
import kotlin.collections.set

/**
 * reference: https://google.github.io/styleguide/jsoncstyleguide.xml, https://github.com/dewitt/opensearch
 */

enum class ResponseMessageType {

    INFO, WARN, ERROR;
}

enum class ResponseCodeType {

    SUCCESS, // NO LOG
    USER_FAILED, // LOG.INFO
    CLIENT_ERROR, // LOG.WARN
    SERVER_ERROR; // LOG.ERROR

    fun isError(): Boolean {
        return this != SUCCESS && this != USER_FAILED
    }
}

suspend fun ApplicationCall.respond(responseDTO: ResponseDTO) {
    respond(responseDTO.code.httpStatusCode, responseDTO)
}

@Serializable
sealed class ResponseDTO {

    abstract val code: ResponseCode

    abstract val data: JsonElement?
}

@Serializable
@SerialName("code")
class CodeResponseDTO(override val code: ResponseCode) : ResponseDTO() {

    companion object {
        val OK = CodeResponseDTO(ResponseCode.OK)
    }

    override val data: JsonElement? = null
}

@OptIn(InternalSerializationApi::class)
@Serializable
@SerialName("data")
class DataResponseDTO(override val code: ResponseCode, override val data: JsonElement) : ResponseDTO() {

    constructor(data: JsonElement) : this(ResponseCode.OK, data)

    companion object {

        inline operator fun <reified T : Any> invoke(data: T): DataResponseDTO {
            return DataResponseDTO(json.encodeToJsonElement(T::class.serializer(), data))
        }

        inline operator fun <reified T : Any> invoke(data: List<T>): DataResponseDTO {
            return DataResponseDTO(JsonArray(data.map { json.encodeToJsonElement(T::class.serializer(), it) }))
        }

        fun longId(id: Long): DataResponseDTO {
            return DataResponseDTO(JsonObject(mapOf("id" to JsonPrimitive(id))))
        }

        fun stringId(id: String): DataResponseDTO {
            return DataResponseDTO(JsonObject(mapOf("id" to JsonPrimitive(id))))
        }

        fun uuid(id: UUID): DataResponseDTO {
            return DataResponseDTO(JsonObject(mapOf("id" to JsonPrimitive(id.toString()))))
        }
    }
}

@OptIn(InternalSerializationApi::class)
@Serializable
@SerialName("paging")
class PagingDataResponseDTO(override val code: ResponseCode, override val data: JsonElement) : ResponseDTO() {

    constructor(data: JsonElement) : this(ResponseCode.OK, data)

    companion object {

        inline fun <reified T : Any> dtoList(offsetLimit: DynamicQuery.OffsetLimit, total: Long, items: List<T>): PagingDataResponseDTO {
            return jsonArray(
                offsetLimit, total, JsonArray(items.map { json.encodeToJsonElement(T::class.serializer(), it) })
            )
        }

        fun jsonArray(offsetLimit: DynamicQuery.OffsetLimit, total: Long, items: JsonArray): PagingDataResponseDTO {
            val totalPages = (total / offsetLimit.itemsPerPage) + (if (total % offsetLimit.itemsPerPage == 0L) 0 else 1)
            val finalItems = if (total != items.size.toLong()) items else {
                val startIndex: Int = (offsetLimit.itemsPerPage * (offsetLimit.pageIndex - 1)).toInt()
                if (startIndex >= items.size) {
                    JsonArray(emptyList())
                } else {
                    var endIndex: Int = (startIndex + offsetLimit.itemsPerPage)
                    if (endIndex > items.size) {
                        endIndex = items.size
                    }
                    JsonArray(items.subList(startIndex, endIndex))
                }
            }
            return PagingDataResponseDTO(
                json.encodeToJsonElement(
                    PagingData.serializer(),
                    PagingData(total, totalPages, offsetLimit.itemsPerPage, offsetLimit.pageIndex, finalItems)
                )
            )
        }
    }

    @Serializable
    class PagingData(
        val total: Long, val totalPages: Long,
        val itemsPerPage: Int, val pageIndex: Long,
        val items: JsonArray
    )
}

@Serializable
@SerialName("error")
class ErrorResponseDTO(
    override val code: ResponseCode,
    val message: String,
    val messageType: ResponseMessageType,
    val detail: String,
    val reqId: String,
    override val data: JsonObject? = null,
    val errors: MutableList<ErrorResponseDetailError>? = null
) : ResponseDTO() {

    @Serializer(forClass = ErrorResponseDTO::class)
    companion object : KSerializer<ErrorResponseDTO> {

        operator fun invoke(ex: BaseException, call: ApplicationCall): ErrorResponseDTO {
            return ErrorResponseDTO(
                ex.code, ResponseUtils.buildUserMessage(ex, call),
                ex.code.messageType, ResponseUtils.buildClientErrorDetailMessage(ex), call.callId!!,
                ex.dataMap?.toJsonObject(), null
            )
        }

        override fun serialize(encoder: Encoder, value: ErrorResponseDTO) {
            val jsonOutput =
                encoder as? JsonEncoder ?: throw SerializationException("This class can be saved only by Json")

            val contentMap: MutableMap<String, Any> = with(value) {
                val map: MutableMap<String, Any> = mutableMapOf(
                    "code" to code.value,
                    "codeType" to code.codeType.name,
                    "messageType" to code.messageType.name,
                    "message" to message,
                    "detail" to detail,
                    "reqId" to reqId
                )
                data?.let { map["data"] = it }
                errors?.let { map["errors"] = it }
                map
            }

            // FIXME => "type": "kotlin.collections.LinkedHashMap", => should be error (override polymorphic descriptor?)
            jsonOutput.encodeJsonElement(JsonObject(mapOf("error" to contentMap.toJsonObject())))
        }

        override fun deserialize(decoder: Decoder): ErrorResponseDTO {
            throw InternalServerErrorException(ResponseCode.UNSUPPORTED_OPERATION_ERROR, "serialize response only")
        }
    }
}

@Serializable
class ErrorResponseDetailError(
    val code: ResponseCode,
    val detail: String,
    val data: JsonObject? = null
) {
    val codeType: ResponseCodeType

    init {
        codeType = code.codeType
    }
}

@OptIn(InternalSerializationApi::class)
@Serializable
@SerialName("dataMessage")
class DataMessageResponseDTO(
    override val code: ResponseCode,
    val message: String,
    val messageType: ResponseMessageType,
    override val data: JsonElement
) : ResponseDTO() {

    constructor(
        code: ResponseCode, args: Map<String, Any>? = null,
        call: ApplicationCall, data: JsonElement? = JsonObject(mapOf())
    )
            : this(code, I18nUtils.getCodeMessage(code, args, call), code.messageType, data!!)

    companion object {

        inline operator fun <reified T : Form<*>> invoke(
            code: ResponseCode, args: Map<String, Any>? = null,
            call: ApplicationCall, data: T
        ): DataMessageResponseDTO {
            return DataMessageResponseDTO(code, args, call, json.encodeToJsonElement(T::class.serializer(), data))
        }

        inline operator fun <reified T : Form<*>> invoke(
            code: ResponseCode, args: Map<String, Any>? = null,
            call: ApplicationCall, data: List<T>
        ): DataMessageResponseDTO {
            return DataMessageResponseDTO(
                code,
                args,
                call,
                JsonArray(data.map { json.encodeToJsonElement(T::class.serializer(), it) })
            )
        }
    }
}

@Serializable
@SerialName("batch")
class BatchResponseDTO(
    override val code: ResponseCode,
    val message: String,
    val messageType: ResponseMessageType,
    override val data: JsonObject
) : ResponseDTO() {
    companion object {

        operator fun invoke(code: ResponseCode, args: Map<String, Any>? = null, call: ApplicationCall): BatchResponseDTO {
            // ENHANCEMENT
            throw InternalServerErrorException(ResponseCode.NOT_IMPLEMENTED_ERROR)
        }
    }
}

@Serializable
class BatchResult(val successes: MutableList<SuccessResult>, val failures: MutableList<FailureResult>)

@OptIn(InternalSerializationApi::class)
@Serializable
class SuccessResult(override val id: String, val data: JsonElement? = JsonObject(mapOf())) :
    IdentifiableObject<String>() {

    companion object {

        inline operator fun <reified T : Form<*>> invoke(id: String, data: T): SuccessResult {
            return SuccessResult(id, json.encodeToJsonElement(T::class.serializer(), data))
        }

        inline operator fun <reified T : Form<*>> invoke(id: String, data: List<T>): SuccessResult {
            return SuccessResult(id, JsonArray(data.map { json.encodeToJsonElement(T::class.serializer(), it) }))
        }
    }
}

@Serializable
class FailureResult(override val id: String, val errors: MutableList<ErrorResponseDetailError>) : IdentifiableObject<String>()

object ResponseUtils {

    fun buildUserErrorMessage(code: ResponseCode, call: ApplicationCall): String {
        return I18nUtils.getCodeTypeMessage(code.codeType, call)
    }

    fun buildUserMessage(ex: BaseException, call: ApplicationCall): String {
        return if (ex.code.codeType.isError())
            buildUserErrorMessage(ex.code, call)
        else {
            if (ex is EntityException) {
                val messageArgsMap: MutableMap<String, Any> = mutableMapOf()

                if (ex.entity != null) {
                    messageArgsMap.putAll(ex.entity.toNotNullMap("entity"))
                }

                if (ex.dataMap != null) {
                    messageArgsMap.putAll(ex.dataMap)
                }

                I18nUtils.getCodeMessage(ex.code, messageArgsMap, call)
            } else {
                I18nUtils.getCodeMessage(ex.code, ex.dataMap, call)
            }

        }
    }

    fun buildClientErrorDetailMessage(ex: BaseException): String =
        if (ex.code.codeType == ResponseCodeType.SERVER_ERROR) "" else ex.message ?: ""
}