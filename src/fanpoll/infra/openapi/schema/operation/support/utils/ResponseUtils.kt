/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.schema.operation.support.utils

import fanpoll.infra.ResponseCode
import fanpoll.infra.openapi.OpenApiConfig
import fanpoll.infra.utils.I18nUtils

object ResponseUtils {

    private const val SWAGGER_NEWLINE = "<br><br>"

    fun buildResponseCodesDescription(responseCodes: List<ResponseCode>): String {
        return responseCodes.groupBy { it.codeType }.mapValues { entry ->
            entry.value.joinToString(SWAGGER_NEWLINE) { buildResponseCodeDescription(it) }
        }.map { "[${it.key.name}] $SWAGGER_NEWLINE ${it.value}" }.joinToString(SWAGGER_NEWLINE)
    }

    fun buildResponseCodeDescription(code: ResponseCode): String {
        return "${code.value} => ${code.name} (${code.httpStatusCode.value}) ${
            I18nUtils.getCodeMessageOrNull(code, null, OpenApiConfig.langCode) ?: ""
        }"
    }
}