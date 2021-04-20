/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.httpclient

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder

// should be only used by HttpClientManager
interface HttpClientService {

    val id: String

    val clientConfig: CIOHttpClientConfig

    val defaultRequest: (HttpRequestBuilder.() -> Unit)?

    var client: HttpClient
}