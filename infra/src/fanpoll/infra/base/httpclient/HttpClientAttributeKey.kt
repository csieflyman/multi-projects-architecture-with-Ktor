/*
 * Copyright (c) 2022. fanpoll All rights reserved.
 */

package fanpoll.infra.base.httpclient

import io.ktor.http.HttpHeaders
import io.ktor.util.AttributeKey
import java.util.*

object HttpClientAttributeKey {

    val ID = AttributeKey<UUID>(HttpHeaders.XRequestId)
}