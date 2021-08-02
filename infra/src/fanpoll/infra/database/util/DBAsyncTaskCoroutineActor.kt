/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.database.util

import fanpoll.infra.base.async.CoroutineActor
import fanpoll.infra.base.async.CoroutineActorConfig
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.logging.error.ErrorLog
import fanpoll.infra.logging.writers.LogWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class DBAsyncTaskCoroutineActor(
    coroutineActorConfig: CoroutineActorConfig,
    private val logWriter: LogWriter
) {

    private val logger = KotlinLogging.logger {}

    private val actorName = "DBAsyncTaskActor"

    private val actor: CoroutineActor<DBAsyncTask> = CoroutineActor(
        actorName, Channel.UNLIMITED,
        coroutineActorConfig, Dispatchers.IO,
        this::execute, null,
        logWriter
    )

    fun run(type: String, block: Transaction.() -> Any?): UUID {
        val task = DBAsyncTask(type, block)
        actor.sendToUnlimitedChannel(task, InfraResponseCode.DB_ASYNC_TASK_ERROR) // non-blocking by Channel.UNLIMITED
        return task.id
    }

    private fun execute(task: DBAsyncTask) {
        try {
            transaction {
                task.block(this)
            }
        } catch (e: Throwable) {
            val errorMsg = "$actorName execute error"
            logger.error("errorMsg => $task", e)
            logWriter.write(
                ErrorLog.internal(
                    InternalServerException(
                        InfraResponseCode.DB_ASYNC_TASK_ERROR, errorMsg, e,
                        mapOf("taskId" to task.id, "taskType" to task.type)
                    ),
                    actorName, task.id.toString()
                )
            )
        }
    }

    internal fun shutdown() {
        actor.close()
    }
}