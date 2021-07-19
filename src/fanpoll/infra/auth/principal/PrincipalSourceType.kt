/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.principal

enum class PrincipalSourceType {

    System, Postman, Ops,
    Browser, Android, iOS;

    fun isApp(): Boolean = this == Android || this == iOS
}