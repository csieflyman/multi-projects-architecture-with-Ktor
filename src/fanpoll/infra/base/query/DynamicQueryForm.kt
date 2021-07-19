/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.query

import fanpoll.infra.base.form.Form
import kotlinx.serialization.Serializable

@Serializable
class DynamicQueryForm(
    val fields: List<String>? = null,
    val filter: String? = null,
    val orderBy: String? = null,
    val offset: Long? = null,
    val limit: Int? = null,
    val pageIndex: Long? = null,
    val itemsPerPage: Int? = null,
    val count: Boolean?,
    val paramMap: MutableMap<String, String>? = null
) : Form<DynamicQueryForm>()