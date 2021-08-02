/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.club

import fanpoll.infra.base.response.ResponseCode
import fanpoll.infra.base.response.ResponseCodeType
import io.ktor.http.HttpStatusCode
import kotlin.reflect.full.memberProperties

object ClubResponseCode {

    val CLUB_ERROR = ResponseCode("CLUB_ERROR", "4000", ResponseCodeType.SERVER_ERROR, HttpStatusCode.InternalServerError)

    val AllCodes = ClubResponseCode::class.memberProperties
        .filter { it.returnType == ResponseCode.KTypeOf }
        .map { it.getter.call(this) as ResponseCode }
        .sortedWith(ResponseCode.ValueComparator)
}