/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.location

import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.extension.receiveUTF8Text
import fanpoll.infra.base.form.Form
import fanpoll.infra.base.json.json
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.base.tenant.TenantForm
import fanpoll.infra.base.tenant.TenantId
import fanpoll.infra.base.tenant.TenantIdLocation
import io.konform.validation.Invalid
import io.konform.validation.Validation
import io.ktor.application.ApplicationCall
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

abstract class Location {

    protected open fun <L : Location> validator(): Validation<L>? = null

    open fun validate(form: Form<*>? = null) {
        val result = validator<Location>()?.validate(this)
        if (result is Invalid<Location>)
            throw RequestException(InfraResponseCode.BAD_REQUEST_PATH_OR_QUERYSTRING, result)
    }
}

@OptIn(InternalSerializationApi::class)
suspend inline fun <reified T : Form<*>> ApplicationCall.receiveAndValidateBody(location: Location? = null): T {
    val form = try {
        json.decodeFromString(T::class.serializer(), receiveUTF8Text())
    } catch (e: Throwable) {
        throw RequestException(InfraResponseCode.BAD_REQUEST_BODY, "can't deserialize from json: ${e.message}", e)
    }
    form.validate()
    if (form is TenantForm<*>) {
        attributes.put(TenantId.ATTRIBUTE_KEY, form.tenantId)
    }
    if (location != null) {
        location.validate(form)
        if (location is TenantIdLocation) {
            attributes.put(TenantId.ATTRIBUTE_KEY, location.tenantId)
        }
    }
    return form
}