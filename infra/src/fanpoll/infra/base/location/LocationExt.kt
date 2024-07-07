/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.base.location

import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.extension.receiveUTF8Text
import fanpoll.infra.base.form.Form
import fanpoll.infra.base.json.kotlinx.json
import fanpoll.infra.base.response.InfraResponseCode
import io.ktor.server.application.ApplicationCall
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

@OptIn(InternalSerializationApi::class)
suspend inline fun <reified T : Form<*>> ApplicationCall.receiveAndValidateBody(location: Location? = null): T {
    val form = try {
        json.decodeFromString(T::class.serializer(), receiveUTF8Text())
    } catch (e: Throwable) {
        throw RequestException(InfraResponseCode.BAD_REQUEST_BODY_FORMAT, "can't deserialize from json: ${e.message}", e)
    }
    form.validate()
    location?.validate(form)
    return form
}