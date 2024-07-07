/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.logging

import fanpoll.infra.base.json.kotlinx.json
import fanpoll.infra.base.util.IdentifiableObject
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import java.time.Instant
import java.util.*

abstract class LogEntity : IdentifiableObject<UUID>() {

    abstract val traceId: String?

    abstract val project: String

    abstract val occurAt: Instant

    abstract val type: String

    abstract val level: LogLevel

    fun toJson(): JsonElement = json.encodeToJsonElement(this)
}