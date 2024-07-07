/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.release.app.service

import fanpoll.infra.auth.login.ClientVersionCheckResult
import fanpoll.infra.auth.principal.ClientAttributeKey
import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.release.app.domain.AppVersion
import fanpoll.infra.release.app.repository.AppReleaseRepository
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.header

class AppReleaseChecker(private val appReleaseRepository: AppReleaseRepository) {

    suspend fun check(call: ApplicationCall): ClientVersionCheckResult? {
        return call.attributes.getOrNull(ClientAttributeKey.CHECK_RESULT) ?: run {
            val principalSource = call.attributes[PrincipalSource.ATTRIBUTE_KEY]
            if (principalSource.checkClientVersion &&
                call.attributes.contains(ClientAttributeKey.CLIENT_VERSION)
            ) {
                val clientVersion = call.attributes[ClientAttributeKey.CLIENT_VERSION]
                val appVersion = AppVersion(principalSource.id, principalSource.clientSource.toAppOS()!!, clientVersion)

                val result = check(appVersion)
                call.attributes.put(ClientAttributeKey.CHECK_RESULT, result)
                call.response.header(ClientAttributeKey.CHECK_RESULT.name, result.name)
                result
            } else null
        }
    }

    suspend fun check(appVersion: AppVersion): ClientVersionCheckResult {
        val forceUpdateMap = appReleaseRepository.checkForceUpdate(appVersion)
        val result = when {
            forceUpdateMap.isEmpty() -> ClientVersionCheckResult.Latest
            forceUpdateMap.count() == 1 -> if (forceUpdateMap.values.first()) ClientVersionCheckResult.ForceUpdate else ClientVersionCheckResult.Update
            else -> ClientVersionCheckResult.ForceUpdate
        }
        return result
    }
}