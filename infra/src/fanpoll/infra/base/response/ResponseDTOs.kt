/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

@file:OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)

package fanpoll.infra.base.response

import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.form.Form
import fanpoll.infra.base.json.json
import fanpoll.infra.base.query.DynamicQuery
import fanpoll.infra.base.util.IdentifiableObject
import io.ktor.application.ApplicationCall
import io.ktor.response.respond
import kotlinx.serialization.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.*

suspend fun ApplicationCall.respond(responseDTO: ResponseDTO) {
    respond(responseDTO.code.httpStatusCode, responseDTO)
}

@Serializable
sealed class ResponseDTO {

    abstract val code: ResponseCode
    abstract val message: String?
    abstract val data: JsonElement?
}

@Serializable
@SerialName("code")
class CodeResponseDTO(override val code: ResponseCode) : ResponseDTO() {

    override val message: String? = null
    override val data: JsonElement? = null

    companion object {
        val OK = CodeResponseDTO(InfraResponseCode.OK)
    }
}

@Serializable
@SerialName("data")
class DataResponseDTO(
    override val code: ResponseCode,
    override val message: String? = null,
    override val data: JsonElement
) : ResponseDTO() {

    companion object {

        inline operator fun <reified T : Any> invoke(data: T, message: String? = null): DataResponseDTO =
            DataResponseDTO(InfraResponseCode.OK, message, json.encodeToJsonElement(T::class.serializer(), data))

        inline operator fun <reified T : Any> invoke(data: List<T>, message: String? = null): DataResponseDTO =
            DataResponseDTO(InfraResponseCode.OK, message, JsonArray(data.map { json.encodeToJsonElement(T::class.serializer(), it) }))

        fun longId(id: Long): DataResponseDTO =
            DataResponseDTO(InfraResponseCode.OK, data = JsonObject(mapOf("id" to JsonPrimitive(id))))

        fun stringId(id: String): DataResponseDTO =
            DataResponseDTO(InfraResponseCode.OK, data = JsonObject(mapOf("id" to JsonPrimitive(id))))

        fun uuid(id: UUID): DataResponseDTO =
            DataResponseDTO(InfraResponseCode.OK, data = JsonObject(mapOf("id" to JsonPrimitive(id.toString()))))
    }
}

@Serializable
@SerialName("paging")
class PagingDataResponseDTO(
    override val code: ResponseCode,
    override val message: String? = null,
    override val data: JsonElement
) : ResponseDTO() {

    companion object {

        inline fun <reified T : Any> dtoList(
            offsetLimit: DynamicQuery.OffsetLimit,
            total: Long,
            items: List<T>,
            message: String? = null
        ): PagingDataResponseDTO {
            return jsonArray(
                offsetLimit, total, JsonArray(items.map { json.encodeToJsonElement(T::class.serializer(), it) }), message
            )
        }

        fun jsonArray(
            offsetLimit: DynamicQuery.OffsetLimit,
            total: Long,
            items: JsonArray,
            message: String? = null
        ): PagingDataResponseDTO {
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
                InfraResponseCode.OK,
                data = json.encodeToJsonElement(
                    PagingData.serializer(),
                    PagingData(total, totalPages, offsetLimit.itemsPerPage, offsetLimit.pageIndex, finalItems)
                ),
                message = message
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
    override val message: String,
    val detail: String,
    val reqId: String,
    override val data: JsonObject? = null,
    val errors: MutableList<ErrorResponseDetailError>? = null
) : ResponseDTO()

@Serializable
class ErrorResponseDetailError(
    val code: ResponseCode,
    val detail: String,
    val data: JsonObject? = null
)

@Serializable
@SerialName("batch")
class BatchResponseDTO(
    override val code: ResponseCode,
    override val message: String,
    override val data: JsonObject
) : ResponseDTO() {

    companion object {

        operator fun invoke(code: ResponseCode, args: Map<String, Any>? = null, call: ApplicationCall): BatchResponseDTO {
            // ENHANCEMENT
            throw InternalServerException(InfraResponseCode.NOT_IMPLEMENTED_ERROR)
        }
    }
}

@Serializable
class BatchResult(val successes: MutableList<SuccessResult>, val failures: MutableList<FailureResult>)

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