/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club

import fanpoll.club.auth.loadAuthModule
import fanpoll.club.club.loadClubModule
import fanpoll.club.database.loadClubDatabaseModule
import fanpoll.club.notification.loadNotificationModule
import fanpoll.club.user.loadUserModule
import io.ktor.server.application.Application
import org.koin.core.Koin
import org.koin.core.component.KoinComponent
import org.koin.dsl.koinApplication

fun Application.loadProjectModules(config: ClubConfig) {
    ClubKoinContext.init()
    loadBaseModule()
    loadClubDatabaseModule(config.databases)
    loadUserModule()
    loadAuthModule(config.auth)
    loadClubModule()
    loadNotificationModule()
}

object ClubKoinContext {

    lateinit var koin: Koin
    fun init() {
        koin = koinApplication {}.koin
    }
}

abstract class ClubKoinComponent : KoinComponent {
    final override fun getKoin(): Koin = ClubKoinContext.koin
    fun getInfraKoin(): Koin = super.getKoin()
}