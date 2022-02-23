/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth

import io.ktor.util.AttributeKey

val ATTRIBUTE_KEY_CLIENT_ID = AttributeKey<String>("clientId")

const val HEADER_CLIENT_VERSION = "clientVersion"

const val HEADER_CLIENT_VERSION_CHECK_RESULT = "clientVersionCheckResult"

val ATTRIBUTE_KEY_CLIENT_VERSION = AttributeKey<String>(HEADER_CLIENT_VERSION)

val ATTRIBUTE_KEY_CLIENT_VERSION_RESULT = AttributeKey<ClientVersionCheckResult>(HEADER_CLIENT_VERSION_CHECK_RESULT)

enum class ClientVersionCheckResult {

    Latest, Update, ForceUpdate
}