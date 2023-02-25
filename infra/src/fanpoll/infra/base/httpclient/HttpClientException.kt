/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.httpclient

import fanpoll.infra.base.exception.RemoteServiceException
import fanpoll.infra.base.extension.toInstant
import fanpoll.infra.base.response.ResponseCode
import fanpoll.infra.base.tenant.TenantId
import io.ktor.client.request.HttpRequest
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.content.TextContent
import io.ktor.http.formUrlEncode
import kotlinx.coroutines.runBlocking
import java.time.Duration

class HttpClientException(
    request: HttpRequest,
    code: ResponseCode,
    message: String? = null,
    cause: Throwable? = null,
    dataMap: Map<String, Any>? = null,
    tenantId: TenantId? = null,
    serviceId: String
) : RemoteServiceException(
    code, message, cause, dataMap, tenantId,
    name = serviceId,
    api = request.apiLogString(),
    reqId = request.attributes[HttpClientAttributeKey.REQ_ID],
    reqAt = request.attributes[HttpClientAttributeKey.REQ_AT],
    reqBody = request.textBody()
)

fun HttpRequest.apiLogString(): String = "${method.value} - $url"

fun HttpRequest.textBody(): String? = when (content) {
    is TextContent -> (content as TextContent).text
    is FormDataContent -> (content as FormDataContent).formData.formUrlEncode()
    else -> null
}

fun HttpResponse.duration(): Duration = Duration.between(requestTime.toInstant(), responseTime.toInstant())

fun HttpResponse.bodyAsTextBlocking(): String = runBlocking { bodyAsText() }