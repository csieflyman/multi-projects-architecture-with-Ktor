package fanpoll.infra.redis.ktorio.utils

import kotlinx.coroutines.CompletableDeferred

/**
 * Use [block] to complete [deferred], also handles [block] exceptions
 */
inline fun <T> completeWith(deferred: CompletableDeferred<T>, block: () -> T) {
    try {
        deferred.complete(block())
    } catch (cause: Throwable) {
        deferred.completeExceptionally(cause)
    }
}
