/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.response

import io.ktor.http.HttpStatusCode
import kotlin.reflect.full.memberProperties

object InfraResponseCode {

    // ========== General ==========
    val OK = ResponseCode("OK", "0000", ResponseCodeType.SUCCESS, HttpStatusCode.OK)

    // Entity Validation
    val ENTITY_NOT_FOUND =
        ResponseCode("ENTITY_NOT_FOUND", "0010", ResponseCodeType.CLIENT_INFO, HttpStatusCode.UnprocessableEntity) // get, find
    val ENTITY_NOT_EXIST =
        ResponseCode("ENTITY_NOT_EXIST", "0011", ResponseCodeType.CLIENT_INFO, HttpStatusCode.UnprocessableEntity) // update, delete
    val ENTITY_ALREADY_EXISTS =
        ResponseCode("ENTITY_ALREADY_EXISTS", "0012", ResponseCodeType.CLIENT_INFO, HttpStatusCode.UnprocessableEntity) // create
    val ENTITY_PROP_VALUE_INVALID =
        ResponseCode("ENTITY_PROP_VALUE_INVALID", "0013", ResponseCodeType.CLIENT_INFO, HttpStatusCode.UnprocessableEntity) // update
    val ENTITY_STATUS_DISABLED =
        ResponseCode("ENTITY_STATUS_DISABLED", "0014", ResponseCodeType.CLIENT_INFO, HttpStatusCode.Conflict) // update
    val ENTITY_STATUS_CONFLICT =
        ResponseCode("ENTITY_STATUS_CONFLICT", "0015", ResponseCodeType.CLIENT_INFO, HttpStatusCode.Conflict) // update
    val ENTITY_VERSION_CONFLICT =
        ResponseCode("ENTITY_VERSION_CONFLICT", "0016", ResponseCodeType.CLIENT_INFO, HttpStatusCode.Conflict) // update
    val ENTITY_READ_FORBIDDEN = ResponseCode("ENTITY_READ_FORBIDDEN", "0017", ResponseCodeType.CLIENT_INFO, HttpStatusCode.Forbidden)
    val ENTITY_UPDATE_FORBIDDEN = ResponseCode("ENTITY_UPDATE_FORBIDDEN", "0018", ResponseCodeType.CLIENT_INFO, HttpStatusCode.Forbidden)
    val ENTITY_DELETE_FORBIDDEN = ResponseCode("ENTITY_DELETE_FORBIDDEN", "0019", ResponseCodeType.CLIENT_INFO, HttpStatusCode.Forbidden)

    // Query Result Validation
    val QUERY_RESULT_EMPTY = ResponseCode("QUERY_RESULT_EMPTY", "0030", ResponseCodeType.CLIENT_INFO, HttpStatusCode.OK)
    val QUERY_RESULT_SINGLE = ResponseCode("QUERY_RESULT_SINGLE", "0031", ResponseCodeType.CLIENT_INFO, HttpStatusCode.OK)
    val QUERY_RESULT_MULTIPLE = ResponseCode("QUERY_RESULT_MULTIPLE", "0032", ResponseCodeType.CLIENT_INFO, HttpStatusCode.OK)
    val QUERY_RESULT_NON_EMPTY = ResponseCode("QUERY_RESULT_NON_EMPTY", "0033", ResponseCodeType.CLIENT_INFO, HttpStatusCode.OK)

    // Batch Processing
    val BATCH_SUCCESS = ResponseCode("BATCH_SUCCESS", "0034", ResponseCodeType.SUCCESS, HttpStatusCode.OK)
    val BATCH_RESULT_FAILURE = ResponseCode("BATCH_RESULT_FAILURE", "0035", ResponseCodeType.CLIENT_INFO, HttpStatusCode.OK)

    // ========== Bad Request ==========
    val BAD_REQUEST_HEADER = ResponseCode("BAD_REQUEST_HEADER", "1000", ResponseCodeType.CLIENT_ERROR, HttpStatusCode.BadRequest)
    val BAD_REQUEST_PATH = ResponseCode("BAD_REQUEST_PATH", "1001", ResponseCodeType.CLIENT_ERROR, HttpStatusCode.BadRequest)
    val BAD_REQUEST_QUERYSTRING = ResponseCode("BAD_REQUEST_QUERYSTRING", "1002", ResponseCodeType.CLIENT_ERROR, HttpStatusCode.BadRequest)
    val BAD_REQUEST_PATH_OR_QUERYSTRING =
        ResponseCode("BAD_REQUEST_PATH_OR_QUERYSTRING", "1003", ResponseCodeType.CLIENT_ERROR, HttpStatusCode.BadRequest)
    val BAD_REQUEST_BODY = ResponseCode("BAD_REQUEST_BODY", "1004", ResponseCodeType.CLIENT_ERROR, HttpStatusCode.BadRequest)
    val BAD_REQUEST = ResponseCode("BAD_REQUEST", "1005", ResponseCodeType.CLIENT_ERROR, HttpStatusCode.BadRequest)
    val BAD_REQUEST_LANG = ResponseCode("BAD_REQUEST_LANG", "1006", ResponseCodeType.CLIENT_ERROR, HttpStatusCode.BadRequest)
    val BAD_REQUEST_TENANT_ID = ResponseCode("BAD_REQUEST_HASH_ID", "1007", ResponseCodeType.CLIENT_ERROR, HttpStatusCode.BadRequest)
    val BAD_REQUEST_HASH_ID = ResponseCode("BAD_REQUEST_HASH_ID", "1008", ResponseCodeType.CLIENT_ERROR, HttpStatusCode.BadRequest)
    val BAD_REQUEST_APP_VERSION = ResponseCode("BAD_REQUEST_APP_VERSION", "1009", ResponseCodeType.CLIENT_ERROR, HttpStatusCode.BadRequest)

    // ========== Authentication ==========
    val AUTH_BAD_KEY = ResponseCode("AUTH_BAD_KEY", "2000", ResponseCodeType.CLIENT_ERROR, HttpStatusCode.Unauthorized)
    val AUTH_BAD_SOURCE = ResponseCode("AUTH_BAD_SOURCE", "2001", ResponseCodeType.CLIENT_ERROR, HttpStatusCode.Unauthorized)
    val AUTH_NO_PRINCIPAL = ResponseCode("AUTH_BAD_SOURCE", "2002", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError)
    val AUTH_USER_TYPE_FORBIDDEN = ResponseCode("AUTH_USER_TYPE_FORBIDDEN", "2003", ResponseCodeType.CLIENT_ERROR, HttpStatusCode.Forbidden)
    val AUTH_ROLE_FORBIDDEN = ResponseCode("AUTH_ROLE_FORBIDDEN", "2004", ResponseCodeType.CLIENT_ERROR, HttpStatusCode.Forbidden)
    val AUTH_SESSION_NOT_FOUND = ResponseCode("AUTH_SESSION_NOT_FOUND", "2005", ResponseCodeType.CLIENT_INFO, HttpStatusCode.Unauthorized)
    val AUTH_PRINCIPAL_DISABLED = ResponseCode("AUTH_PRINCIPAL_DISABLED", "2006", ResponseCodeType.CLIENT_INFO, HttpStatusCode.Forbidden)
    val AUTH_TENANT_DISABLED = ResponseCode("AUTH_TENANT_DISABLED", "2007", ResponseCodeType.CLIENT_INFO, HttpStatusCode.Forbidden)
    val AUTH_LOGIN_UNAUTHENTICATED =
        ResponseCode("AUTH_LOGIN_UNAUTHENTICATED", "2008", ResponseCodeType.CLIENT_INFO, HttpStatusCode.Unauthorized)
    val AUTH_BAD_PASSWORD = ResponseCode("AUTH_BAD_PASSWORD", "2009", ResponseCodeType.CLIENT_INFO, HttpStatusCode.Unauthorized)
    val AUTH_SESSION_INVALID = ResponseCode("AUTH_SESSION_INVALID", "2010", ResponseCodeType.CLIENT_INFO, HttpStatusCode.Unauthorized)
    val AUTH_TENANT_FORBIDDEN = ResponseCode("AUTH_TENANT_FORBIDDEN", "2011", ResponseCodeType.CLIENT_INFO, HttpStatusCode.Forbidden)
    val AUTH_BAD_HOST = ResponseCode("AUTH_BAD_HOST", "2012", ResponseCodeType.CLIENT_ERROR, HttpStatusCode.Unauthorized)

    // ========== Business Logic ==========

    // ========== RemoteService Error ==========
    val REMOTE_SERVICE_CONNECT_ERROR =
        ResponseCode("REMOTE_SERVICE_CONNECT_ERROR", "8000", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError)
    val REMOTE_SERVICE_CONNECT_TIMEOUT_ERROR =
        ResponseCode("REMOTE_SERVICE_CONNECT_TIMEOUT_ERROR", "8001", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError)
    val REMOTE_SERVICE_REQUEST_TIMEOUT_ERROR =
        ResponseCode("REMOTE_SERVICE_REQUEST_TIMEOUT_ERROR", "8002", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError)
    val REMOTE_SERVICE_SOCKET_TIMEOUT_ERROR =
        ResponseCode("REMOTE_SERVICE_SOCKET_TIMEOUT_ERROR", "8003", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError)
    val REMOTE_SERVICE_RESPONSE_STATUS_ERROR =
        ResponseCode("REMOTE_SERVICE_RESPONSE_STATUS_ERROR", "8004", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError)
    val REMOTE_SERVICE_RESPONSE_BODY_PARSE_ERROR =
        ResponseCode("REMOTE_SERVICE_RESPONSE_BODY_PARSE_ERROR", "8005", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError)

    val REDIS_ERROR = ResponseCode("REDIS_ERROR", "8010", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError)
    val AWS_KINESIS_ERROR = ResponseCode("AWS_KINESIS_ERROR", "8011", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError)
    val SES_ERROR = ResponseCode("SES_ERROR", "8012", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError)
    val FCM_ERROR = ResponseCode("FCM_ERROR", "8013", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError)
    val SMS_MITAKE_ERROR = ResponseCode("SMS_MITAKE_ERROR", "8014", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError)

    // ========== Internal Server Error ==========
    val SERVER_CONFIG_ERROR = ResponseCode("SERVER_CONFIG_ERROR", "9000", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError)
    val COROUTINE_ERROR = ResponseCode("COROUTINE_ERROR", "9001", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError)
    val LOG_ERROR = ResponseCode("LOG_ERROR", "9002", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError)
    val DB_ERROR = ResponseCode("DB_ERROR", "9003", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError)
    val DB_SQL_ERROR = ResponseCode("DB_SQL_ERROR", "9004", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError)
    val CACHE_ERROR = ResponseCode("CACHE_ERROR", "9005", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError)
    val REDIS_KEY_NOTIFICATION_ERROR =
        ResponseCode("REDIS_KEY_NOTIFICATION_ERROR", "9006", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError)
    val NOTIFICATION_ERROR = ResponseCode("NOTIFICATION_ERROR", "9007", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError)
    val IO_ERROR = ResponseCode("IO_ERROR", "9008", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError)
    val DB_ASYNC_TASK_ERROR = ResponseCode("DB_ASYNC_TASK_ERROR", "9009", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError)

    // Data Processing Error
    val DATA_JSON_INVALID = ResponseCode("DATA_JSON_INVALID", "9100", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError)

    // Programming Error
    val OPENAPI_ERROR = ResponseCode("OPENAPI_ERROR", "9995", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError)
    val DEV_ERROR = ResponseCode("DEV_ERROR", "9996", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError)
    val NOT_IMPLEMENTED_ERROR =
        ResponseCode("NOT_IMPLEMENTED_ERROR", "9997", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError)
    val UNSUPPORTED_OPERATION_ERROR =
        ResponseCode("UNSUPPORTED_OPERATION_ERROR", "9998", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError)

    val UNEXPECTED_ERROR = ResponseCode("UNEXPECTED_ERROR", "9999", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError)

    val AllCodes = InfraResponseCode::class.memberProperties
        .filter { it.returnType == ResponseCode.KTypeOf }
        .map { it.getter.call(this) as ResponseCode }
        .sortedWith(ResponseCode.ValueComparator)
}