/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.database.util

import fanpoll.infra.base.util.IdentifiableObject
import org.jetbrains.exposed.sql.Transaction
import java.util.*

class DBAsyncTask(val type: String, val block: Transaction.() -> Any?) : IdentifiableObject<UUID>() {

    override val id: UUID = UUID.randomUUID()

    override fun toString(): String {
        return "$type/$id"
    }
}