/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.base.entity

import java.io.Serializable

interface Identifiable<ID : Comparable<ID>> : Serializable {

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