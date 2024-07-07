/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.report.data.table

import fanpoll.infra.base.util.IdentifiableObject
import kotlinx.serialization.Serializable

@Serializable
class DataColumn(override val id: String, val name: String = id, val valueType: String) : IdentifiableObject<String>() {

}