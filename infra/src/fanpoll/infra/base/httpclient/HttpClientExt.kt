/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.base.httpclient

import fanpoll.infra.base.extension.toInstant
import io.ktor.client.request.HttpRequest
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.content.TextContent
import io.ktor.http.formUrlEncode
import kotlinx.coroutines.runBlocking
import java.time.Duration

fun HttpRequest.apiLogString(): String = "${method.value} - $url"

fun HttpRequest.textBody(): String? = when (content) {
    is TextContent -> (content as TextContent).text
    is FormDataContent -> (content as FormDataContent).formData.formUrlEncode()
    else -> null
}

fun HttpResponse.duration(): Duration = Duration.between(requestTime.toInstant(), responseTime.toInstant())

fun HttpResponse.bodyAsTextBlocking(): String = runBlocking { bodyAsText() }