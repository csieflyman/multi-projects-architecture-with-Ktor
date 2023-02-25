/*
 * Copyright (c) 2022. fanpoll All rights reserved.
 */

package fanpoll.infra.base.httpclient

import io.ktor.http.HttpHeaders
import io.ktor.util.AttributeKey
import java.time.Instant

object HttpClientAttributeKey {

    //val TRACE_ID = AttributeKey<String>("traceId") // optional
    val REQ_ID = AttributeKey<String>(HttpHeaders.XRequestId)
    val REQ_AT = AttributeKey<Instant>("reqAt")
}