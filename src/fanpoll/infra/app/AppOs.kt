/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.app

import fanpoll.infra.auth.UserDeviceType

enum class AppOs {

    Android, iOS;

    companion object {

        fun from(userDeviceType: UserDeviceType): AppOs = when (userDeviceType) {
            UserDeviceType.Android -> Android
            UserDeviceType.iOS -> iOS
            else -> error("invalid AppOs from UserDeviceType: $userDeviceType")
        }

    }

    fun toUserDeviceType(): UserDeviceType = when (this) {
        Android -> UserDeviceType.Android
        iOS -> UserDeviceType.iOS
    }
}