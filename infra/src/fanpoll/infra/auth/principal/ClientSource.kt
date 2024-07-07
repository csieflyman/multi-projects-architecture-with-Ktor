/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.principal

import fanpoll.infra.release.app.domain.AppOS

enum class ClientSource {
    Service, Browser, Android, iOS;

    fun isUserLogin(): Boolean {
        return this == Browser || this == Android || this == iOS
    }

    fun toAppOS(): AppOS? {
        return when (this) {
            Android -> AppOS.Android
            iOS -> AppOS.iOS
            else -> null
        }
    }
}