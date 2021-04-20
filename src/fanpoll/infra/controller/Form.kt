/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.controller

import fanpoll.infra.RequestBodyException
import fanpoll.infra.model.Entity
import io.konform.validation.Invalid
import io.konform.validation.Validation

abstract class Form<Self> {

    open fun validator(): Validation<Self>? = null

    open fun validate() {
        val validator = validator()
        val result = validator?.validate(this as Self)
        if (result is Invalid)
            throw RequestBodyException(result)
    }
}

abstract class EntityForm<Self, DID : Any, EID : Comparable<EID>> : Form<Self>() {

    open fun getDtoId(): DID? = null

    open fun getEntityId(): EID? = null

    fun getId(): Any = tryGetId() ?: error("${javaClass.name} Both entityId and dtoId can't be null")

    fun tryGetId(): Any? = getEntityId() ?: getDtoId()

    open fun toEntity(): Entity<EID> = TODO() // subclass override
}

abstract class TenantForm<Self> : Form<Self>() {

    abstract val tenantId: String

    override fun validate() {
        try {
            super.validate()
        } catch (e: RequestBodyException) {
            e.tenantId = tenantId
            throw e
        }
    }
}