/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel.push.token

import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.database.exposed.InfraRepositoryComponent
import fanpoll.infra.database.exposed.sql.dbExecute
import fanpoll.infra.database.exposed.sql.insert
import fanpoll.infra.database.exposed.sql.singleOrNull
import fanpoll.infra.database.exposed.sql.toList
import fanpoll.infra.logging.error.ErrorLog
import fanpoll.infra.logging.writers.LogWriter
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.update
import java.util.*

class DevicePushTokenExposedRepository(private val logWriter: LogWriter) : InfraRepositoryComponent(), DevicePushTokenRepository {

    private val logger = KotlinLogging.logger {}

    override suspend fun create(devicePushToken: DevicePushToken) {
        dbExecute(infraDatabase) {
            val dbEntity = DevicePushTokenTable.select(DevicePushTokenTable.id, DevicePushTokenTable.pushToken)
                .where { DevicePushTokenTable.id eq devicePushToken.deviceId }
                .singleOrNull(DevicePushToken::class)
            if (dbEntity == null) {
                DevicePushTokenTable.insert(devicePushToken)
            } else {
                if (dbEntity.pushToken != devicePushToken.pushToken) {
                    DevicePushTokenTable.update({ DevicePushTokenTable.id eq devicePushToken.deviceId }) {
                        it[pushToken] = devicePushToken.pushToken!!
                    }
                } else {
                    null
                }
            }
        }
    }

    override suspend fun getUserPushTokens(userIds: List<UUID>): Map<UUID, List<String>> {
        return dbExecute(infraDatabase) {
            DevicePushTokenTable
                .select(DevicePushTokenTable.id, DevicePushTokenTable.userId, DevicePushTokenTable.pushToken)
                .where { DevicePushTokenTable.userId inList userIds }
                .toList(DevicePushToken::class)
                .groupBy(keySelector = { it.userId!! }, valueTransform = { it.pushToken!! })
        }
    }

    override fun deleteUnRegisteredTokens(tokens: Collection<String>): Unit = runBlocking {
        try {
            dbExecute(infraDatabase) {
                DevicePushTokenTable.deleteWhere { pushToken inList tokens.toSet() }
            }
        } catch (e: Throwable) {
            val errorMsg = "deleteUnRegisteredTokens error"
            logger.error(e) { "$errorMsg => $tokens" }
            logWriter.write(
                ErrorLog.internal(
                    InternalServerException(InfraResponseCode.NOTIFICATION_ERROR, errorMsg, e, mapOf("tokens" to tokens)),
                    "DevicePushTokenRepository", null
                )
            )
        }
    }
}