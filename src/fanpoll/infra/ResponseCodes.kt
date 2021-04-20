/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package fanpoll.infra

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
enum class ResponseCode(
    val value: String, val codeType: ResponseCodeType,
    val messageType: ResponseMessageType,
    val httpStatusCode: HttpStatusCode
) {

    // ========== General ==========
    // Success
    OK("0000", ResponseCodeType.SUCCESS, ResponseMessageType.INFO, HttpStatusCode.OK),
    CREATED("0001", ResponseCodeType.SUCCESS, ResponseMessageType.INFO, HttpStatusCode.OK),
    UPDATED("0002", ResponseCodeType.SUCCESS, ResponseMessageType.INFO, HttpStatusCode.OK),
    DELETED("0003", ResponseCodeType.SUCCESS, ResponseMessageType.INFO, HttpStatusCode.OK),
    ADDED("0004", ResponseCodeType.SUCCESS, ResponseMessageType.INFO, HttpStatusCode.OK),
    REMOVED("0005", ResponseCodeType.SUCCESS, ResponseMessageType.INFO, HttpStatusCode.OK),

    // Entity Validation (Client Error ? => we assume client should check entity first now)
    ENTITY_NOT_FOUND("0010", ResponseCodeType.CLIENT_ERROR, ResponseMessageType.INFO, HttpStatusCode.BadRequest),
    ENTITY_DUPLICATED("0011", ResponseCodeType.CLIENT_ERROR, ResponseMessageType.INFO, HttpStatusCode.BadRequest),
    ENTITY_DISABLED("0012", ResponseCodeType.CLIENT_ERROR, ResponseMessageType.INFO, HttpStatusCode.BadRequest),
    ENTITY_NOT_FOUND_OR_DISABLED(
        "0013",
        ResponseCodeType.CLIENT_ERROR,
        ResponseMessageType.INFO,
        HttpStatusCode.BadRequest
    ),
    ENTITY_PROP_INVALID("0014", ResponseCodeType.CLIENT_ERROR, ResponseMessageType.INFO, HttpStatusCode.BadRequest),
    ENTITY_STATUS_INVALID("0015", ResponseCodeType.CLIENT_ERROR, ResponseMessageType.INFO, HttpStatusCode.BadRequest),
    ENTITY_FORBIDDEN("0016", ResponseCodeType.USER_FAILED, ResponseMessageType.INFO, HttpStatusCode.Forbidden),
    ENTITY_JSON_INVALID("0017", ResponseCodeType.CLIENT_ERROR, ResponseMessageType.ERROR, HttpStatusCode.BadRequest),

    // Query Result Validation
    QUERY_RESULT_EMPTY("0020", ResponseCodeType.USER_FAILED, ResponseMessageType.INFO, HttpStatusCode.OK),
    QUERY_RESULT_SINGLE("0021", ResponseCodeType.USER_FAILED, ResponseMessageType.INFO, HttpStatusCode.OK),
    QUERY_RESULT_MULTIPLE("0022", ResponseCodeType.USER_FAILED, ResponseMessageType.INFO, HttpStatusCode.OK),
    QUERY_RESULT_NON_EMPTY("0023", ResponseCodeType.USER_FAILED, ResponseMessageType.INFO, HttpStatusCode.OK),

    // Batch Processing
    BATCH_ERROR("0030", ResponseCodeType.SERVER_ERROR, ResponseMessageType.ERROR, HttpStatusCode.InternalServerError),
    BATCH_SUCCESS("0031", ResponseCodeType.SUCCESS, ResponseMessageType.INFO, HttpStatusCode.OK),
    BATCH_RESULT_FAILURE("0032", ResponseCodeType.USER_FAILED, ResponseMessageType.INFO, HttpStatusCode.OK),

    // ========== Bad Request ==========
    REQUEST_BAD_HEADER("1000", ResponseCodeType.CLIENT_ERROR, ResponseMessageType.ERROR, HttpStatusCode.BadRequest),
    REQUEST_BAD_BODY("1001", ResponseCodeType.CLIENT_ERROR, ResponseMessageType.ERROR, HttpStatusCode.BadRequest),
    REQUEST_BAD_PATH("1002", ResponseCodeType.CLIENT_ERROR, ResponseMessageType.ERROR, HttpStatusCode.BadRequest),
    REQUEST_BAD_QUERY("1003", ResponseCodeType.CLIENT_ERROR, ResponseMessageType.ERROR, HttpStatusCode.BadRequest),
    REQUEST_BAD_PATH_OR_QUERY(
        "1004",
        ResponseCodeType.CLIENT_ERROR,
        ResponseMessageType.ERROR,
        HttpStatusCode.BadRequest
    ),
    REQUEST_BAD_ALL("1005", ResponseCodeType.CLIENT_ERROR, ResponseMessageType.ERROR, HttpStatusCode.BadRequest),
    REQUEST_BAD_LANG("1006", ResponseCodeType.CLIENT_ERROR, ResponseMessageType.ERROR, HttpStatusCode.BadRequest),
    REQUEST_BAD_TENANT_ID("1007", ResponseCodeType.CLIENT_ERROR, ResponseMessageType.ERROR, HttpStatusCode.BadRequest),
    REQUEST_BAD_USER_ID("1008", ResponseCodeType.CLIENT_ERROR, ResponseMessageType.ERROR, HttpStatusCode.BadRequest),
    REQUEST_BAD_HASH_ID("1009", ResponseCodeType.CLIENT_ERROR, ResponseMessageType.ERROR, HttpStatusCode.BadRequest),

    // ========== Bad Request (App) ==========
    REQUEST_BAD_APP_VERSION(
        "1100",
        ResponseCodeType.CLIENT_ERROR,
        ResponseMessageType.ERROR,
        HttpStatusCode.BadRequest
    ),

    // ========== Authentication ==========
    AUTH_BAD_KEY("2000", ResponseCodeType.CLIENT_ERROR, ResponseMessageType.ERROR, HttpStatusCode.Unauthorized),
    AUTH_BAD_SOURCE("2001", ResponseCodeType.CLIENT_ERROR, ResponseMessageType.ERROR, HttpStatusCode.Unauthorized),
    AUTH_NO_PRINCIPAL(
        "2002",
        ResponseCodeType.SERVER_ERROR,
        ResponseMessageType.ERROR,
        HttpStatusCode.InternalServerError
    ),
    AUTH_USER_TYPE_FORBIDDEN(
        "2003",
        ResponseCodeType.CLIENT_ERROR,
        ResponseMessageType.ERROR,
        HttpStatusCode.Forbidden
    ),
    AUTH_ROLE_FORBIDDEN("2004", ResponseCodeType.CLIENT_ERROR, ResponseMessageType.ERROR, HttpStatusCode.Forbidden),
    AUTH_SESSION_NOT_FOUND("2005", ResponseCodeType.USER_FAILED, ResponseMessageType.INFO, HttpStatusCode.Unauthorized),
    AUTH_PRINCIPAL_DISABLED("2006", ResponseCodeType.USER_FAILED, ResponseMessageType.INFO, HttpStatusCode.Forbidden),
    AUTH_TENANT_DISABLED("2007", ResponseCodeType.USER_FAILED, ResponseMessageType.INFO, HttpStatusCode.Forbidden),
    AUTH_LOGIN_UNAUTHENTICATED(
        "2008",
        ResponseCodeType.USER_FAILED,
        ResponseMessageType.INFO,
        HttpStatusCode.Unauthorized
    ),
    AUTH_BAD_PASSWORD("2009", ResponseCodeType.USER_FAILED, ResponseMessageType.INFO, HttpStatusCode.Unauthorized),
    AUTH_SESSION_INVALID("2010", ResponseCodeType.USER_FAILED, ResponseMessageType.INFO, HttpStatusCode.Unauthorized),
    AUTH_TENANT_FORBIDDEN("2011", ResponseCodeType.USER_FAILED, ResponseMessageType.INFO, HttpStatusCode.Forbidden),
    AUTH_BAD_HOST("2012", ResponseCodeType.CLIENT_ERROR, ResponseMessageType.ERROR, HttpStatusCode.Unauthorized),

    // ========== Business Logic ==========

    // ========== RemoteService Error ==========
    REMOTE_SERVICE_CONNECT_ERROR(
        "8000",
        ResponseCodeType.SERVER_ERROR,
        ResponseMessageType.ERROR,
        HttpStatusCode.InternalServerError
    ),
    REMOTE_SERVICE_CONNECT_TIMEOUT_ERROR(
        "8001",
        ResponseCodeType.SERVER_ERROR,
        ResponseMessageType.ERROR,
        HttpStatusCode.InternalServerError
    ),
    REMOTE_SERVICE_REQUEST_TIMEOUT_ERROR(
        "8002",
        ResponseCodeType.SERVER_ERROR,
        ResponseMessageType.ERROR,
        HttpStatusCode.InternalServerError
    ),
    REMOTE_SERVICE_SOCKET_TIMEOUT_ERROR(
        "8003",
        ResponseCodeType.SERVER_ERROR,
        ResponseMessageType.ERROR,
        HttpStatusCode.InternalServerError
    ),
    REMOTE_SERVICE_RESPONSE_STATUS_ERROR(
        "8004",
        ResponseCodeType.SERVER_ERROR,
        ResponseMessageType.ERROR,
        HttpStatusCode.InternalServerError
    ),
    REMOTE_SERVICE_RESPONSE_BODY_PARSE_ERROR(
        "8005",
        ResponseCodeType.SERVER_ERROR,
        ResponseMessageType.ERROR,
        HttpStatusCode.InternalServerError
    ),

    REDIS_ERROR("8010", ResponseCodeType.SERVER_ERROR, ResponseMessageType.ERROR, HttpStatusCode.InternalServerError),
    AWS_KINESIS_ERROR("8011", ResponseCodeType.SERVER_ERROR, ResponseMessageType.ERROR, HttpStatusCode.InternalServerError),
    SES_ERROR("8012", ResponseCodeType.SERVER_ERROR, ResponseMessageType.ERROR, HttpStatusCode.InternalServerError),
    FCM_ERROR("8013", ResponseCodeType.SERVER_ERROR, ResponseMessageType.ERROR, HttpStatusCode.InternalServerError),
    SMS_MITAKE_ERROR("8014", ResponseCodeType.SERVER_ERROR, ResponseMessageType.ERROR, HttpStatusCode.InternalServerError),

    // ========== Internal Server Error ==========
    SERVER_CONFIG_ERROR(
        "9000",
        ResponseCodeType.SERVER_ERROR,
        ResponseMessageType.ERROR,
        HttpStatusCode.InternalServerError
    ),
    COROUTINE_ERROR(
        "9001",
        ResponseCodeType.SERVER_ERROR,
        ResponseMessageType.ERROR,
        HttpStatusCode.InternalServerError
    ),
    LOG_ERROR("9002", ResponseCodeType.SERVER_ERROR, ResponseMessageType.ERROR, HttpStatusCode.InternalServerError),
    DB_ERROR("9003", ResponseCodeType.SERVER_ERROR, ResponseMessageType.ERROR, HttpStatusCode.InternalServerError),
    DB_SQL_ERROR("9004", ResponseCodeType.SERVER_ERROR, ResponseMessageType.ERROR, HttpStatusCode.InternalServerError),
    CACHE_ERROR("9005", ResponseCodeType.SERVER_ERROR, ResponseMessageType.ERROR, HttpStatusCode.InternalServerError),
    REDIS_KEY_NOTIFICATION_ERROR("9006", ResponseCodeType.SERVER_ERROR, ResponseMessageType.ERROR, HttpStatusCode.InternalServerError),
    NOTIFICATION_ERROR("9007", ResponseCodeType.SERVER_ERROR, ResponseMessageType.ERROR, HttpStatusCode.InternalServerError),
    IO_ERROR("9008", ResponseCodeType.SERVER_ERROR, ResponseMessageType.ERROR, HttpStatusCode.InternalServerError),

    DB_ASYNC_ERROR(
        "9100",
        ResponseCodeType.SERVER_ERROR,
        ResponseMessageType.ERROR,
        HttpStatusCode.InternalServerError
    ),

    // Programming Error
    OPENAPI_ERROR("9995", ResponseCodeType.SERVER_ERROR, ResponseMessageType.ERROR, HttpStatusCode.InternalServerError),
    DEV_ERROR("9996", ResponseCodeType.SERVER_ERROR, ResponseMessageType.ERROR, HttpStatusCode.InternalServerError),
    NOT_IMPLEMENTED_ERROR(
        "9997",
        ResponseCodeType.SERVER_ERROR,
        ResponseMessageType.ERROR,
        HttpStatusCode.InternalServerError
    ),
    UNSUPPORTED_OPERATION_ERROR(
        "9998",
        ResponseCodeType.SERVER_ERROR,
        ResponseMessageType.ERROR,
        HttpStatusCode.InternalServerError
    ),

    UNEXPECTED_ERROR(
        "9999",
        ResponseCodeType.SERVER_ERROR,
        ResponseMessageType.ERROR,
        HttpStatusCode.InternalServerError
    );

    @Serializer(forClass = ResponseCode::class)
    companion object : KSerializer<ResponseCode> {

        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("fanpoll.infra.ResponseCode", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: ResponseCode) {
            encoder.encodeString(value.value)
        }

        override fun deserialize(decoder: Decoder): ResponseCode {
            throw InternalServerErrorException(NOT_IMPLEMENTED_ERROR)
        }
    }
}