/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.database.exposed

import fanpoll.club.ClubKoinComponent
import org.jetbrains.exposed.sql.Database
import org.koin.core.qualifier.named

abstract class ClubRepositoryComponent : ClubKoinComponent() {

    val clubDatabase = getKoin().get<Database>(named(ClubDatabase.Club.name))
}