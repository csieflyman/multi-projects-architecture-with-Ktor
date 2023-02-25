/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

@file:OptIn(KtorExperimentalLocationsAPI::class)

package fanpoll.infra.base.query

import fanpoll.infra.base.location.Location
import io.ktor.server.locations.KtorExperimentalLocationsAPI

@io.ktor.server.locations.Location("")
data class DynamicQueryLocation(
    val q_fields: String? = null,
    val q_filter: String? = null,
    val q_orderBy: String? = null,
    val q_offset: Long? = null,
    val q_limit: Int? = null,
    val q_pageIndex: Long? = null,
    val q_itemsPerPage: Int? = null,
    val q_count: Boolean? = null
) : Location()