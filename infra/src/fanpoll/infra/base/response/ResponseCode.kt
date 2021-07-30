/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package fanpoll.infra.base.response

import fanpoll.infra.base.exception.InternalServerException
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
enum class ResponseCode(val value: String, val codeType: ResponseCodeType, val httpStatusCode: HttpStatusCode) {

    // ========== General ==========
    OK("0000", ResponseCodeType.SUCCESS, HttpStatusCode.OK),

    // Entity Validation
    ENTITY_NOT_FOUND("0010", ResponseCodeType.CLIENT_INFO, HttpStatusCode.UnprocessableEntity), // get, find
    ENTITY_NOT_EXIST("0011", ResponseCodeType.CLIENT_INFO, HttpStatusCode.UnprocessableEntity), // update, delete
    ENTITY_ALREADY_EXISTS("0012", ResponseCodeType.CLIENT_INFO, HttpStatusCode.UnprocessableEntity), // create
    ENTITY_PROP_VALUE_INVALID("0013", ResponseCodeType.CLIENT_INFO, HttpStatusCode.UnprocessableEntity), // update
    ENTITY_STATUS_DISABLED("0014", ResponseCodeType.CLIENT_INFO, HttpStatusCode.Conflict), // update
    ENTITY_STATUS_CONFLICT("0015", ResponseCodeType.CLIENT_INFO, HttpStatusCode.Conflict), // update
    ENTITY_VERSION_CONFLICT("0016", ResponseCodeType.CLIENT_INFO, HttpStatusCode.Conflict), // update
    ENTITY_READ_FORBIDDEN("0017", ResponseCodeType.CLIENT_INFO, HttpStatusCode.Forbidden),
    ENTITY_UPDATE_FORBIDDEN("0018", ResponseCodeType.CLIENT_INFO, HttpStatusCode.Forbidden),
    ENTITY_DELETE_FORBIDDEN("0019", ResponseCodeType.CLIENT_INFO, HttpStatusCode.Forbidden),

    // Query Result Validation
    QUERY_RESULT_EMPTY("0030", ResponseCodeType.CLIENT_INFO, HttpStatusCode.OK),
    QUERY_RESULT_SINGLE("0031", ResponseCodeType.CLIENT_INFO, HttpStatusCode.OK),
    QUERY_RESULT_MULTIPLE("0032", ResponseCodeType.CLIENT_INFO, HttpStatusCode.OK),
    QUERY_RESULT_NON_EMPTY("0033", ResponseCodeType.CLIENT_INFO, HttpStatusCode.OK),

    // Batch Processing
    BATCH_SUCCESS("0034", ResponseCodeType.SUCCESS, HttpStatusCode.OK),
    BATCH_RESULT_FAILURE("0035", ResponseCodeType.CLIENT_INFO, HttpStatusCode.OK),

    // ========== Bad Request ==========
    BAD_REQUEST_HEADER("1000", ResponseCodeType.CLIENT_ERROR, HttpStatusCode.BadRequest),
    BAD_REQUEST_PATH("1001", ResponseCodeType.CLIENT_ERROR, HttpStatusCode.BadRequest),
    BAD_REQUEST_QUERYSTRING("1002", ResponseCodeType.CLIENT_ERROR, HttpStatusCode.BadRequest),
    BAD_REQUEST_PATH_OR_QUERYSTRING("1003", ResponseCodeType.CLIENT_ERROR, HttpStatusCode.BadRequest),
    BAD_REQUEST_BODY("1004", ResponseCodeType.CLIENT_ERROR, HttpStatusCode.BadRequest),
    BAD_REQUEST("1005", ResponseCodeType.CLIENT_ERROR, HttpStatusCode.BadRequest),
    BAD_REQUEST_LANG("1006", ResponseCodeType.CLIENT_ERROR, HttpStatusCode.BadRequest),
    BAD_REQUEST_TENANT_ID("1007", ResponseCodeType.CLIENT_ERROR, HttpStatusCode.BadRequest),
    BAD_REQUEST_HASH_ID("1008", ResponseCodeType.CLIENT_ERROR, HttpStatusCode.BadRequest),
    BAD_REQUEST_APP_VERSION("1009", ResponseCodeType.CLIENT_ERROR, HttpStatusCode.BadRequest),

    // ========== Authentication ==========
    AUTH_BAD_KEY("2000", ResponseCodeType.CLIENT_ERROR, HttpStatusCode.Unauthorized),
    AUTH_BAD_SOURCE("2001", ResponseCodeType.CLIENT_ERROR, HttpStatusCode.Unauthorized),
    AUTH_NO_PRINCIPAL("2002", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError),
    AUTH_USER_TYPE_FORBIDDEN("2003", ResponseCodeType.CLIENT_ERROR, HttpStatusCode.Forbidden),
    AUTH_ROLE_FORBIDDEN("2004", ResponseCodeType.CLIENT_ERROR, HttpStatusCode.Forbidden),
    AUTH_SESSION_NOT_FOUND("2005", ResponseCodeType.CLIENT_INFO, HttpStatusCode.Unauthorized),
    AUTH_PRINCIPAL_DISABLED("2006", ResponseCodeType.CLIENT_INFO, HttpStatusCode.Forbidden),
    AUTH_TENANT_DISABLED("2007", ResponseCodeType.CLIENT_INFO, HttpStatusCode.Forbidden),
    AUTH_LOGIN_UNAUTHENTICATED("2008", ResponseCodeType.CLIENT_INFO, HttpStatusCode.Unauthorized),
    AUTH_BAD_PASSWORD("2009", ResponseCodeType.CLIENT_INFO, HttpStatusCode.Unauthorized),
    AUTH_SESSION_INVALID("2010", ResponseCodeType.CLIENT_INFO, HttpStatusCode.Unauthorized),
    AUTH_TENANT_FORBIDDEN("2011", ResponseCodeType.CLIENT_INFO, HttpStatusCode.Forbidden),
    AUTH_BAD_HOST("2012", ResponseCodeType.CLIENT_ERROR, HttpStatusCode.Unauthorized),

    // ========== Business Logic ==========

    // ========== RemoteService Error ==========
    REMOTE_SERVICE_CONNECT_ERROR("8000", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError),
    REMOTE_SERVICE_CONNECT_TIMEOUT_ERROR("8001", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError),
    REMOTE_SERVICE_REQUEST_TIMEOUT_ERROR("8002", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError),
    REMOTE_SERVICE_SOCKET_TIMEOUT_ERROR("8003", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError),
    REMOTE_SERVICE_RESPONSE_STATUS_ERROR("8004", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError),
    REMOTE_SERVICE_RESPONSE_BODY_PARSE_ERROR("8005", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError),

    REDIS_ERROR("8010", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError),
    AWS_KINESIS_ERROR("8011", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError),
    SES_ERROR("8012", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError),
    FCM_ERROR("8013", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError),
    SMS_MITAKE_ERROR("8014", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError),

    // ========== Internal Server Error ==========
    SERVER_CONFIG_ERROR("9000", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError),
    COROUTINE_ERROR("9001", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError),
    LOG_ERROR("9002", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError),
    DB_ERROR("9003", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError),
    DB_SQL_ERROR("9004", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError),
    CACHE_ERROR("9005", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError),
    REDIS_KEY_NOTIFICATION_ERROR("9006", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError),
    NOTIFICATION_ERROR("9007", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError),
    IO_ERROR("9008", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError),
    DB_ASYNC_TASK_ERROR("9009", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError),

    // Data Processing Error
    DATA_JSON_INVALID("9100", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError),

    // Programming Error
    OPENAPI_ERROR("9995", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError),
    DEV_ERROR("9996", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError),
    NOT_IMPLEMENTED_ERROR("9997", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError),
    UNSUPPORTED_OPERATION_ERROR("9998", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError),

    UNEXPECTED_ERROR("9999", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError);

    @Serializer(forClass = ResponseCode::class)
    companion object : KSerializer<ResponseCode> {

        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("fanpoll.infra.base.response.ResponseCode", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: ResponseCode) {
            encoder.encodeString(value.value)
        }

        override fun deserialize(decoder: Decoder): ResponseCode {
            throw InternalServerException(NOT_IMPLEMENTED_ERROR)
        }
    }

    fun isError(): Boolean = codeType.isError()
}