/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.location

import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.form.Form
import fanpoll.infra.base.response.InfraResponseCode
import io.konform.validation.Invalid
import io.konform.validation.Validation

abstract class Location {

    protected open fun <L : Location> validator(): Validation<L>? = null

    open fun validate(form: Form<*>? = null) {
        val result = validator<Location>()?.validate(this)
        if (result is Invalid)
            throw RequestException(InfraResponseCode.BAD_REQUEST_PATH_OR_QUERYSTRING, result)
    }
}