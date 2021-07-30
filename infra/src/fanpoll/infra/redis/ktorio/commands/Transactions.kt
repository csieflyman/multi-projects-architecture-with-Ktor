/**
 * https://redis.io/topics/transactions
 */
package fanpoll.infra.redis.ktorio.commands

import fanpoll.infra.redis.ktorio.Redis

/**
 * Discard all commands issued after MULTI
 *
 * https://redis.io/commands/discard
 *
 * @since 2.0.0
 */
internal suspend fun Redis.discard(): Unit = executeTyped("DISCARD")

/**
 * Execute all commands issued after MULTI
 *
 * https://redis.io/commands/exec
 *
 * @since 1.2.0
 */
internal suspend fun Redis.exec(): Unit = executeTyped("EXEC")

/**
 * Mark the start of a transaction block
 *
 * https://redis.io/commands/multi
 *
 * @since 1.2.0
 */
internal suspend fun Redis.multi(): Unit = executeTyped("MULTI")

/**
 * Forget about all watched keys
 *
 * https://redis.io/commands/unwatch
 *
 * @since 2.2.0
 */
internal suspend fun Redis.unwatch(): Unit = executeTyped("UNWATCH")

/**
 * Watch the given keys to determine execution of the MULTI/EXEC block
 *
 * https://redis.io/commands/watch
 *
 * @since 2.2.0
 */
internal suspend fun Redis.watch(vararg keys: String): Unit = executeTyped("WATCH", *keys)

/**
 * Executes a transaction.
 * - If no exception is thrown, all the commands executed inside callback will be commited
 * - If an exception is thrown, all the commands executed inside callback will be discarded and the exception rethrown
 *
 * https://redis.io/topics/transactions
 *
 * @since 2.2.0
 */
internal suspend inline fun Redis.transaction(vararg keys: String, callback: () -> Unit) {
    multi()
    if (keys.isNotEmpty()) watch(*keys)
    try {
        callback()
        exec()
    } catch (e: Throwable) {
        discard()
        throw e
    }
}
