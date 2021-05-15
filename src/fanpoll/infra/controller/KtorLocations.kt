/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

@file:OptIn(KtorExperimentalLocationsAPI::class)

package fanpoll.infra.controller

import fanpoll.infra.RequestException
import fanpoll.infra.ResponseCode
import fanpoll.infra.auth.UserPrincipal
import fanpoll.infra.model.TenantId
import fanpoll.infra.utils.DateTimeUtils
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
import java.time.*
import java.util.*

abstract class MyLocation {

    protected open fun <L : MyLocation> validator(): Validation<L>? = null

    open fun validate(form: Form<*>? = null) {
        val result = validator<MyLocation>()?.validate(this)
        if (result is Invalid<MyLocation>)
            throw RequestException(result)
    }
}

abstract class EntityIdLocation : MyLocation() {

    abstract val entityId: Any

    override fun validate(form: Form<*>?) {
        if (form is EntityForm<*, *, *> && form.getEntityId() != entityId)
            throw RequestException(ResponseCode.REQUEST_BAD_PATH, "mismatch entityId between path and body")
        super.validate(form)
    }
}

@Location("/{entityId}")
data class LongEntityIdLocation(override val entityId: Long) : EntityIdLocation()

@Location("/{entityId}")
data class StringEntityIdLocation(override val entityId: String) : EntityIdLocation()

@Location("/{entityId}")
data class UUIDEntityIdLocation(override val entityId: UUID) : EntityIdLocation()

@Location("/{tenantId}")
data class TenantIdLocation(val tenantId: String) : MyLocation() {

    override fun validate(form: Form<*>?) {
        if (form is TenantForm<*> && form.tenantId != tenantId)
            throw RequestException(ResponseCode.REQUEST_BAD_PATH, "mismatch tenantId between path and body")
        try {
            super.validate(form)
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
suspend inline fun <reified T : Form<*>> ApplicationCall.receiveAndValidateBody(location: MyLocation? = null): T {
    val form = json.decodeFromString(T::class.serializer(), receiveUTF8Text())
    form.validate()
    if (form is TenantForm<*>) {
        attributes.put(ATTRIBUTE_KEY_TENANT_ID, TenantId(form.tenantId))
    }
    if (location != null) {
        location.validate(form)
        if (location is TenantIdLocation) {
            attributes.put(ATTRIBUTE_KEY_TENANT_ID, TenantId(location.tenantId))
        }
    }
    return form
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
    convert<ZoneId> {
        decode { values, _ ->
            values.singleOrNull()?.let { ZoneId.of(it) }
        }
        encode { value ->
            listOf((value as ZoneId).id)
        }
    }
    convert<Instant> {
        decode { values, _ ->
            values.singleOrNull()?.let { ZonedDateTime.parse(it, DateTimeUtils.UTC_DATE_TIME_FORMATTER).toInstant() }
        }
        encode { value ->
            listOf(DateTimeUtils.UTC_DATE_TIME_FORMATTER.format(value as Instant))
        }
    }
    convert<ZonedDateTime> {
        decode { values, _ ->
            values.singleOrNull()?.let { ZonedDateTime.parse(it, DateTimeUtils.UTC_DATE_TIME_FORMATTER) }
        }
        encode { value ->
            listOf(DateTimeUtils.UTC_DATE_TIME_FORMATTER.format(value as ZonedDateTime))
        }
    }
    convert<LocalDateTime> {
        decode { values, _ ->
            values.singleOrNull()?.let { LocalDateTime.parse(it, DateTimeUtils.LOCAL_DATE_TIME_FORMATTER) }
        }
        encode { value ->
            listOf(DateTimeUtils.LOCAL_DATE_TIME_FORMATTER.format(value as LocalDateTime))
        }
    }
    convert<LocalDate> {
        decode { values, _ ->
            values.singleOrNull()?.let { LocalDate.parse(it, DateTimeUtils.LOCAL_DATE_FORMATTER) }
        }
        encode { value ->
            listOf(DateTimeUtils.LOCAL_DATE_FORMATTER.format(value as LocalDate))
        }
    }
    convert<LocalTime> {
        decode { values, _ ->
            values.singleOrNull()?.let { LocalTime.parse(it, DateTimeUtils.LOCAL_TIME_FORMATTER) }
        }
        encode { value ->
            listOf(DateTimeUtils.LOCAL_TIME_FORMATTER.format(value as LocalTime))
        }
    }
}