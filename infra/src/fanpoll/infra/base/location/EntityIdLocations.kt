/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

@file:OptIn(KtorExperimentalLocationsAPI::class)

package fanpoll.infra.base.location

import fanpoll.infra.base.entity.EntityForm
import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.form.Form
import fanpoll.infra.base.response.InfraResponseCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import java.util.*

abstract class EntityIdLocation : Location() {

    abstract val entityId: Any

    override fun validate(form: Form<*>?) {
        if (form is EntityForm<*, *, *> && form.getEntityId() != entityId)
            throw RequestException(InfraResponseCode.BAD_REQUEST_PATH, "mismatch entityId between path and body")
        super.validate(form)
    }
}

@io.ktor.locations.Location("/{entityId}")
data class LongEntityIdLocation(override val entityId: Long) : EntityIdLocation()

@io.ktor.locations.Location("/{entityId}")
data class StringEntityIdLocation(override val entityId: String) : EntityIdLocation()

@io.ktor.locations.Location("/{entityId}")
data class UUIDEntityIdLocation(override val entityId: UUID) : EntityIdLocation()