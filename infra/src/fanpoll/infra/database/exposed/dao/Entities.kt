/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.database.exposed.dao

import fanpoll.infra.base.entity.Identifiable
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.id.EntityID
import java.util.*

abstract class IntEntity(id: EntityID<Int>) : IntEntity(id), Identifiable<Int> {

    override fun getId(): Int {
        return id.value
    }

    override fun equals(other: Any?): Boolean = idEquals(other)

    override fun hashCode(): Int = idHashCode()
}

abstract class LongEntity(id: EntityID<Long>) : LongEntity(id), Identifiable<Long> {

    override fun getId(): Long {
        return id.value
    }

    override fun equals(other: Any?): Boolean = idEquals(other)

    override fun hashCode(): Int = idHashCode()
}

abstract class UUIDEntity(id: EntityID<UUID>) : UUIDEntity(id), Identifiable<UUID> {

    override fun getId(): UUID {
        return id.value
    }

    override fun equals(other: Any?): Boolean = idEquals(other)

    override fun hashCode(): Int = idHashCode()
}

abstract class StringEntity(id: EntityID<String>) : Entity<String>(id), Identifiable<String> {

    override fun getId(): String {
        return id.value
    }

    override fun equals(other: Any?): Boolean = idEquals(other)

    override fun hashCode(): Int = idHashCode()
}