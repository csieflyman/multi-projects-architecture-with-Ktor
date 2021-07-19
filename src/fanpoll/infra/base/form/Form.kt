/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.form

import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.response.ResponseCode
import io.konform.validation.Invalid
import io.konform.validation.Validation

abstract class Form<Self> {

    open fun validator(): Validation<Self>? = null

    open fun validate() {
        val validator = validator()
        val result = validator?.validate(this as Self)
        if (result is Invalid)
            throw RequestException(ResponseCode.BAD_REQUEST_BODY, result)
    }
}