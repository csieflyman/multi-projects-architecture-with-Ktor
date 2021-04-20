/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

@file:OptIn(KtorExperimentalLocationsAPI::class)

package fanpoll.infra.controller

import fanpoll.infra.RequestException
import fanpoll.infra.ResponseCode
import fanpoll.infra.auth.UserPrincipal
import fanpoll.infra.model.TenantId
import fanpoll.infra.utils.json
import io.konform.validation.Invalid
import io.konform.validation.Validation
import io.ktor.application.ApplicationCall
import io.ktor.auth.principal
import io.ktor.features.DataConversion
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.locationOrNull
import io.ktor.util.AttributeKey
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import java.util.*

abstract class MyLocation {

    open val entityId: Any? = null

    protected open fun <T : MyLocation> validator(): Validation<T>? = null

    open fun validate() {
        val result = validator<MyLocation>()?.validate(this)
        if (result is Invalid)
            throw RequestException(result)
    }

    fun validate(dto: Form<*>) {
        if (this is TenantIdLocation && dto is TenantForm && dto.tenantId != tenantId)
            throw RequestException(ResponseCode.REQUEST_BAD_PATH, "mismatch tenantId between path and body")
        if (entityId != null && dto is EntityForm<*, *, *> && dto.getEntityId() != entityId)
            throw RequestException(ResponseCode.REQUEST_BAD_PATH, "invalid entity id in path")
        validate()
    }
}

@Location("/{entityId}")
data class LongEntityIdLocation(override val entityId: Long) : MyLocation()

@Location("/{entityId}")
data class StringEntityIdLocation(override val entityId: String) : MyLocation()

@Location("/{entityId}")
data class UUIDEntityIdLocation(override val entityId: UUID) : MyLocation()

@Location("/{tenantId}")
data class TenantIdLocation(val tenantId: String) : MyLocation() {

    override fun validate() {
        try {
            super.validate()
        } catch (e: RequestException) {
            e.tenantId = tenantId
            throw e
        }
    }
}

val ATTRIBUTE_KEY_TENANT_ID = AttributeKey<TenantId>("tenantId")

val ApplicationCall.tenantId: TenantId?
    get() = attributes.getOrNull(ATTRIBUTE_KEY_TENANT_ID)
        ?: principal<UserPrincipal>()?.tenantId
        ?: try {
            TenantId(locationOrNull<TenantIdLocation>().tenantId)
        } catch (e: Exception) {
            null
        }

@OptIn(InternalSerializationApi::class)
suspend inline fun <reified T : Form<*>> ApplicationCall.receiveAndValidate(location: MyLocation? = null): T {
    val dto = json.decodeFromString(T::class.serializer(), receiveUTF8Text())
    dto.validate()
    if (dto is TenantForm<*>) {
        attributes.put(ATTRIBUTE_KEY_TENANT_ID, TenantId(dto.tenantId))
    }
    if (location != null) {
        location.validate(dto)
        if (location is TenantIdLocation) {
            attributes.put(ATTRIBUTE_KEY_TENANT_ID, TenantId(location.tenantId))
        }
    }
    return dto
}

val LocationsDataConverter: DataConversion.Configuration.() -> Unit = {
    convert<UUID> {
        decode { values, _ ->
            values.singleOrNull()?.let { UUID.fromString(it) }
        }
        encode { value ->
            listOf(value.toString())
        }
    }
}