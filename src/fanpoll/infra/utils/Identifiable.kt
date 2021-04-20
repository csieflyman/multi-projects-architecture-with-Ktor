/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.utils

import java.io.Serializable

interface Identifiable<ID : Any> : Serializable {

    fun getId(): ID

    fun idEquals(other: Any?): Boolean {
        if (this === other) return true
        val otherObj = other as? Identifiable<*> ?: return false
        return getId() == otherObj.getId()
    }

    fun idHashCode(): Int = getId().hashCode()
}

abstract class IdentifiableObject<ID : Any> : Serializable {

    abstract val id: ID

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherObj = other as? IdentifiableObject<*> ?: return false
        return id == otherObj.id
    }

    override fun hashCode() = id.hashCode()
    override fun toString() = id.toString()
}