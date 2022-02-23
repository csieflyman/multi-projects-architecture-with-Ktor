/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.app

import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.database.sql.transaction
import fanpoll.infra.logging.error.ErrorLog
import fanpoll.infra.logging.writers.LogWriter
import mu.KotlinLogging
import org.jetbrains.exposed.sql.deleteWhere

class PushTokenStorage(private val logWriter: LogWriter) {

    private val logger = KotlinLogging.logger {}

    fun deleteUnRegisteredTokens(tokens: Collection<String>) {
        try {
            transaction {
                UserDeviceTable.deleteWhere { UserDeviceTable.pushToken inList tokens.toSet() }
            }
        } catch (e: Throwable) {
            val errorMsg = "deleteUnRegisteredTokens error"
            logger.error("$errorMsg => $tokens", e)
            logWriter.write(
                ErrorLog.internal(
                    InternalServerException(InfraResponseCode.NOTIFICATION_ERROR, errorMsg, e, mapOf("tokens" to tokens)),
                    "PushTokenStorage", null
                )
            )
        }
    }
}