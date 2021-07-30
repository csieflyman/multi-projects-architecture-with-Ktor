/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.extension

import io.ktor.application.ApplicationCall
import io.ktor.features.origin
import io.ktor.request.ApplicationRequest
import io.ktor.request.receiveChannel
import io.ktor.util.toByteArray
import kotlinx.coroutines.runBlocking

val ApplicationRequest.publicRemoteHost: String?
    get() = origin.remoteHost.takeIf { host ->
        !host.startsWith("0:0:0:0") &&
                !host.startsWith("10.0.") && !host.startsWith("172.16.") && !host.startsWith("192.168.")
    }

fun ApplicationRequest.fromLocalhost(): Boolean {
    val host = origin.remoteHost
    return host == "localhost" || host == "127.0.0.1" || host == "::1" || host == "0:0:0:0:0:0:0:1"
}

/*
  application/json default charset is not UTF-8 when parsing request => https://github.com/ktorio/ktor/issues/384
  DoubleReceive => https://ktor.io/docs/double-receive.html
  receiveChannel? or receiveStream? which is better?
*/
suspend fun ApplicationCall.receiveUTF8Text(): String {
    return receiveChannel().toByteArray().toString(Charsets.UTF_8)
    //return receiveStream().bufferedReader(charset = Charsets.UTF_8).readText()
}

fun ApplicationCall.bodyString(): String {
    return runBlocking { receiveUTF8Text() }
}