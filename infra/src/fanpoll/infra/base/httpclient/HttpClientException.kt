/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.httpclient

import fanpoll.infra.base.exception.RemoteServiceException
import fanpoll.infra.base.response.ResponseCode
import io.ktor.client.request.HttpRequest

class HttpClientException(
    request: HttpRequest,
    code: ResponseCode,
    message: String? = null,
    cause: Throwable? = null,
    dataMap: Map<String, Any>? = null,
    serviceId: String
) : RemoteServiceException(
    code, message, cause, dataMap,
    name = serviceId,
    api = request.apiLogString(),
    reqId = request.attributes[HttpClientAttributeKey.REQ_ID],
    reqAt = request.attributes[HttpClientAttributeKey.REQ_AT],
    reqBody = request.textBody()
)