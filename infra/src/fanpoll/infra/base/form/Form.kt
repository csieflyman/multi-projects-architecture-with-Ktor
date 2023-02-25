/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.form

import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.response.InfraResponseCode
import io.konform.validation.Invalid
import io.konform.validation.Validation
import kotlinx.serialization.Transient

abstract class Form<Self : Form<Self>> {

    @Transient
    var traceId: String? = null

    open fun validator(): Validation<Self>? = null

    open fun validate() {
        val validator: Validation<Self>? = validator()
        val result = validator?.validate(this as Self)
        if (result is Invalid)
            throw RequestException(InfraResponseCode.BAD_REQUEST_BODY_FIELD, result)
    }
}