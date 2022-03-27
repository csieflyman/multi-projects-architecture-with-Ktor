/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth

import io.ktor.util.AttributeKey

object ClientVersionAttributeKey {

    val CLIENT_ID = AttributeKey<String>("clientId")

    val CLIENT_VERSION = AttributeKey<String>("clientVersion")

    val CHECK_RESULT = AttributeKey<ClientVersionCheckResult>("clientVersionCheckResult")
}

enum class ClientVersionCheckResult {

    Latest, Update, ForceUpdate
}