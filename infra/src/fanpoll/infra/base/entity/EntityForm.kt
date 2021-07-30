/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.entity

import fanpoll.infra.base.form.Form

abstract class EntityForm<Self, DID : Any, EID : Comparable<EID>> : Form<Self>() {

    open fun getDtoId(): DID? = null

    open fun getEntityId(): EID? = null

    fun getId(): Any = tryGetId() ?: error("${javaClass.name} Both entityId and dtoId can't be null")

    fun tryGetId(): Any? = getEntityId() ?: getDtoId()

    open fun toEntity(): Entity<EID> = error("subclass must override this")
}