/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.logging

import io.ktor.http.HttpHeaders
import io.ktor.util.AttributeKey
import java.time.Instant

object RequestAttributeKey {

    val TRACE_ID = AttributeKey<String>("traceId") // optional
    val ID = AttributeKey<String>(HttpHeaders.XRequestId)
    val AT = AttributeKey<Instant>("reqAt")
    val TAGS = AttributeKey<Map<String, String>>("tags") // optional
}

object ResponseAttributeKey {

    val BODY = AttributeKey<String>("rspBody") // optional
}