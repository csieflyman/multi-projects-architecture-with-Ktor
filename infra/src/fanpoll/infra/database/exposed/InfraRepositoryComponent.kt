/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.database.exposed

import org.jetbrains.exposed.sql.Database
import org.koin.core.component.KoinComponent
import org.koin.core.qualifier.named

abstract class InfraRepositoryComponent : KoinComponent {

    val infraDatabase = super.getKoin().get<Database>(named(InfraDatabase.Infra.name))
}