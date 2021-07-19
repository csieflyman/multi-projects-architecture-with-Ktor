/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.util

abstract class IdentifiableObject<ID : Any> {

    abstract val id: ID

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (this.javaClass != other?.javaClass)
            return false
        val otherObj = other as IdentifiableObject<*>
        return id == otherObj.id
    }

    override fun hashCode() = id.hashCode()
}