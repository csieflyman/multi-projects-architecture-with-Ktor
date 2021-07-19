/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.schema.operation.support.utils

import fanpoll.infra.base.response.ResponseCode

object ResponseUtils {

    private const val SWAGGER_NEWLINE = "<br><br>"

    fun buildResponseCodesDescription(codes: List<ResponseCode>): String {
        return codes.groupBy { it.codeType }.mapValues { entry ->
            entry.value.joinToString(SWAGGER_NEWLINE) { buildResponseCodeDescription(it) }
        }.map { "[${it.key.name}] $SWAGGER_NEWLINE ${it.value}" }.joinToString(SWAGGER_NEWLINE)
    }

    fun buildResponseCodeDescription(code: ResponseCode): String {
        return "${code.value} => ${code.name} (${code.httpStatusCode.value})"
    }
}