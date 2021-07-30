/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.logging

import fanpoll.infra.base.json.json
import fanpoll.infra.base.util.IdentifiableObject
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import java.time.Instant
import java.util.*

abstract class LogMessage : IdentifiableObject<UUID>() {

    abstract val occurAt: Instant

    abstract val logType: String

    abstract val logLevel: LogLevel

    fun toJson(): JsonElement = json.encodeToJsonElement(this)
}