/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */
package fanpoll.infra.base.json.kotlinx

import kotlinx.serialization.json.Json

val json = Json {
    ignoreUnknownKeys = true
}