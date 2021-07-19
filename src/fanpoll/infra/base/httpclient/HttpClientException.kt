/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.httpclient

import fanpoll.infra.base.exception.RemoteServiceException
import fanpoll.infra.base.response.ResponseCode
import fanpoll.infra.base.tenant.TenantId
import io.ktor.client.request.HttpRequest
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.client.statement.request
import io.ktor.http.content.TextContent
import io.ktor.http.formUrlEncode
import kotlinx.coroutines.runBlocking

class HttpClientException(
    code: ResponseCode,
    message: String? = null,
    cause: Throwable? = null,
    dataMap: Map<String, Any>? = null,
    tenantId: TenantId? = null,
    serviceId: String,
    response: HttpResponse? = null
) : RemoteServiceException(
    code, message, cause, dataMap, tenantId,
    name = serviceId,
    api = response?.request?.apiLogString() ?: "",
    rspCode = response?.status.toString(),
    reqBody = response?.request?.textBody(),
    rspBody = response?.textBody()
)

fun HttpRequest.apiLogString(): String = "${method.value} - $url"

fun HttpRequest.textBody(): String? = when (content) {
    is TextContent -> (content as TextContent).text
    is FormDataContent -> (content as FormDataContent).formData.formUrlEncode()
    else -> null
}

fun HttpResponse.textBody(): String = runBlocking { readText() }