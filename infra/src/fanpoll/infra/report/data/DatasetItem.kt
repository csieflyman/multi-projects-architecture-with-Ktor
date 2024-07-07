/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.report.data

import fanpoll.infra.base.util.IdentifiableObject
import kotlinx.serialization.json.JsonElement

abstract class DatasetItem : IdentifiableObject<String>() {
    abstract fun toJson(): JsonElement
}