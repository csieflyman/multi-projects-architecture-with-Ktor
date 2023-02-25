/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

@file:OptIn(KtorExperimentalLocationsAPI::class)

package fanpoll.infra.base.tenant

import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.form.Form
import fanpoll.infra.base.location.Location
import fanpoll.infra.base.response.InfraResponseCode
import io.ktor.server.locations.KtorExperimentalLocationsAPI

@io.ktor.server.locations.Location("/{tenantId}")
data class TenantIdLocation(val tenantId: TenantId) : Location() {

    override fun validate(form: Form<*>?) {
        if (form is TenantForm<*> && form.tenantId != tenantId)
            throw RequestException(InfraResponseCode.BAD_REQUEST_PATH, "mismatch tenantId between path and body")
        try {
            super.validate(form)
        } catch (e: RequestException) {
            e.tenantId = tenantId
            throw e
        }
    }
}