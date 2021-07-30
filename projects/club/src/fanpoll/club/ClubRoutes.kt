/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.club

import fanpoll.club.user.clubLogin
import fanpoll.club.user.clubUser
import io.ktor.routing.Routing

fun Routing.club() {

    clubUser()
    clubLogin()
}