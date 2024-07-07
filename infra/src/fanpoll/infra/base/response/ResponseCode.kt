/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.response

import fanpoll.infra.base.extension.myEquals
import fanpoll.infra.base.extension.myHashCode
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.reflect.typeOf

@Serializable
class ResponseCode(
    val name: String,
    val value: String,
    val type: ResponseCodeType,
    @Transient val httpStatusCode: HttpStatusCode = HttpStatusCode.OK
) {
    override fun equals(other: Any?) = myEquals(other, { value })
    override fun hashCode() = myHashCode({ value })
    override fun toString(): String = "$value-$name(${type.name})"

    fun isError(): Boolean = type.isError()

    companion object {

        val KTypeOf = typeOf<ResponseCode>()

        val ValueComparator = Comparator<ResponseCode> { code1, code2 ->
            code1.value.toInt() - code2.value.toInt()
        }
    }
}