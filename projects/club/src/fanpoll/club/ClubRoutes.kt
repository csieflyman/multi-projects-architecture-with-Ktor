/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.club

import fanpoll.club.features.clubLogin
import fanpoll.club.features.clubNotification
import fanpoll.club.features.clubUser
import io.ktor.server.routing.Routing

fun Routing.club() {

    clubUser()
    clubLogin()
    clubNotification()
}