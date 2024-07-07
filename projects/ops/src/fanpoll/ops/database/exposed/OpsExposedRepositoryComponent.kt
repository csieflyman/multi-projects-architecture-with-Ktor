/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.database.exposed

import fanpoll.ops.OpsKoinComponent
import org.jetbrains.exposed.sql.Database
import org.koin.core.qualifier.named

abstract class OpsExposedRepositoryComponent : OpsKoinComponent() {

    val opsDatabase = getKoin().get<Database>(named(OpsDatabase.Ops.name))
}