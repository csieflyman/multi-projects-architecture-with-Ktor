/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.util

import java.io.Serializable

interface Identifiable<ID : Any> : Serializable {

    fun getId(): ID

    fun idEquals(other: Any?): Boolean {
        if (this === other) return true
        if (this.javaClass != other?.javaClass)
            return false
        val otherObj = other as Identifiable<*>
        return getId() == otherObj.getId()
    }

    fun idHashCode(): Int = getId().hashCode()
}