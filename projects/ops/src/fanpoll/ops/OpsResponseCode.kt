/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.ops

import fanpoll.infra.base.response.ResponseCode
import fanpoll.infra.base.response.ResponseCodeType
import io.ktor.http.HttpStatusCode
import kotlin.reflect.full.memberProperties

object OpsResponseCode {

    val OPS_ERROR = ResponseCode("OPS_ERROR", "3000", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError)

    val AllCodes = OpsResponseCode::class.memberProperties
        .filter { it.returnType == ResponseCode.KTypeOf }
        .map { it.getter.call(this) as ResponseCode }
        .sortedWith(ResponseCode.ValueComparator)
}