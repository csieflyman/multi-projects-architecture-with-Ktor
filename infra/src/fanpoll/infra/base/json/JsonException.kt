/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.json

import fanpoll.infra.base.exception.BaseException
import fanpoll.infra.base.response.InfraResponseCode

class JsonException(
    message: String? = null, cause: Throwable? = null,
    val json: String? = null, val path: String? = null
) : BaseException(
    InfraResponseCode.DATA_JSON_INVALID, message, cause,
    mutableMapOf<String, Any>().apply {
        if (json != null)
            put("json", json)
        if (path != null)
            put("path", path)
    }
)