/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.app

import fanpoll.infra.auth.principal.PrincipalSourceType

enum class AppOs {

    Android, iOS;

    companion object {

        fun from(principalSourceType: PrincipalSourceType): AppOs = when (principalSourceType) {
            PrincipalSourceType.Android -> Android
            PrincipalSourceType.iOS -> iOS
            else -> error("invalid AppOs from principalSourceType: $principalSourceType")
        }

    }

    fun principalType(): PrincipalSourceType = when (this) {
        Android -> PrincipalSourceType.Android
        iOS -> PrincipalSourceType.iOS
    }
}