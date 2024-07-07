/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel.push.token

import java.util.*

interface DevicePushTokenRepository {
    suspend fun create(devicePushToken: DevicePushToken)
    suspend fun getUserPushTokens(userIds: List<UUID>): Map<UUID, List<String>>
    fun deleteUnRegisteredTokens(tokens: Collection<String>)
}