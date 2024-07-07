/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.principal

import fanpoll.infra.auth.login.ClientVersionCheckResult
import io.ktor.util.AttributeKey

object ClientAttributeKey {

    val CLIENT_ID = AttributeKey<String>("clientId")

    val CLIENT_VERSION = AttributeKey<String>("clientVersion")

    val CHECK_RESULT = AttributeKey<ClientVersionCheckResult>("clientVersionCheckResult")
}