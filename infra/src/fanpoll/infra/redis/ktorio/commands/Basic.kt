package fanpoll.infra.redis.ktorio.commands

import fanpoll.infra.redis.ktorio.Redis
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlin.reflect.KClass

enum class SortDirection { ASC, DESC }

suspend fun Redis.executeBinary(vararg args: Any?): Any? = execute(*args)
suspend fun Redis.executeText(vararg args: Any?): Any? = execute(*args).byteArraysToString

suspend fun Redis.executeArrayAny(vararg args: Any?): List<Any?> =
    (executeText(*args) as List<Any?>?) ?: listOf()

@Suppress("UNCHECKED_CAST")
suspend fun Redis.executeArrayString(vararg args: Any?): List<String> =
    (executeText(*args) as List<Any?>?)?.filterIsInstance<String>() ?: listOf()

@Suppress("UNCHECKED_CAST")
suspend fun Redis.executeArrayStringNull(vararg args: Any?): List<String?> =
    (executeText(*args) as List<Any?>?) as? List<String?> ?: listOf()

@Suppress("UNCHECKED_CAST")
suspend fun Redis.executeArrayLong(vararg args: Any?): List<Long> =
    (executeText(*args) as List<Long>?) ?: listOf()

// @TODO: Kotlin-JVM doesn't resolve when when inlining, so do not inline it
@Suppress("UNCHECKED_CAST")
suspend fun <T : Any> Redis.executeTypedNull(vararg args: Any?, clazz: KClass<T>): T? = when (clazz) {
    Boolean::class -> ((executeText(*args)?.toString()?.toLongOrNull() ?: 0L) != 0L) as T?
    Unit::class -> run { executeText(*args); Unit as T? }
    Any::class -> run { executeText(*args) as T? }
    String::class -> executeText(*args)?.toString() as T?
    Double::class -> executeText(*args)?.toString()?.toDoubleOrNull() as T?
    Int::class -> executeText(*args)?.toString()?.toIntOrNull() as T?
    Long::class -> executeText(*args)?.toString()?.toLongOrNull() as T?
    ByteArray::class -> executeBinary(*args) as? T?
    else -> error("Unsupported type")
}

// @TODO: Kotlin-JVM doesn't resolve when when inlining, so do not inline it
@Suppress("UNCHECKED_CAST")
suspend fun <T : Any> Redis.executeTyped(vararg args: Any?, clazz: KClass<T>): T = when (clazz) {
    Boolean::class -> ((executeText(*args)?.toString()?.toLongOrNull() ?: 0L) != 0L) as T
    Unit::class -> run { executeText(*args); Unit as T }
    Any::class -> run { (executeText(*args) ?: Unit) as T }
    String::class -> (executeText(*args)?.toString() ?: "") as T
    Double::class -> (executeText(*args)?.toString()?.toDoubleOrNull() ?: 0.0) as T
    Int::class -> (executeText(*args)?.toString()?.toIntOrNull() ?: 0) as T
    Long::class -> (executeText(*args)?.toString()?.toLongOrNull() ?: 0L) as T
    ByteArray::class -> (executeBinary(*args)  ?: byteArrayOf()) as T
    else -> error("Unsupported type")
}

suspend inline fun <reified T : Any> Redis.executeTypedNull(vararg args: Any?): T? = executeTypedNull(*args, clazz = T::class)

suspend inline fun <reified T : Any> Redis.executeTyped(vararg args: Any?): T = executeTyped(*args, clazz = T::class)

suspend inline fun <reified T : Any> Redis.executeBuildNull(
    initialCapacity: Int = 16, callback: ArrayList<Any?>.() -> Unit
): T? = executeTypedNull(*ArrayList<Any?>(initialCapacity).apply(callback).toTypedArray())

suspend inline fun <reified T : Any> Redis.executeBuildNotNull(
    initialCapacity: Int = 16, callback: ArrayList<Any?>.() -> Unit
): T = executeTyped(*ArrayList<Any?>(initialCapacity).apply(callback).toTypedArray())

internal fun <T> List<T>.toListOfPairs(): List<Pair<T, T>> =
    (0 until size / 2).map { this[it * 2 + 0] to this[it * 2 + 1] }

internal fun <T> List<T>.toListOfPairsString(): List<Pair<String, String>> =
    (0 until size / 2).map { ("${this[it * 2 + 0]}") to ("${this[it * 2 + 1]}") }

internal fun List<Any?>.listOfPairsToMap(): Map<String, String> =
    (0 until size / 2).map { ("${this[it * 2 + 0]}") to ("${this[it * 2 + 1]}") }.toMap()

internal fun List<Any?>.listOfPairsToMapAny(): Map<Any?, Any?> =
    (0 until size / 2).map { this[it * 2 + 0] to this[it * 2 + 1] }.toMap()

private val UTF8 = Charsets.UTF_8

private val Any?.byteArraysToString: Any?
    get() = when (this) {
        is ByteArray -> this.toString(UTF8)
        is List<*> -> { // @TODO: Copy only on different instances
            this.map { it.byteArraysToString }.toList()
        }
        is Map<*, *> -> { // @TODO: Copy only on different instances
            this.map { it.key.byteArraysToString to it.value.byteArraysToString }.toMap()
        }
        else -> this
    }

internal inline fun <reified T : Any> arrayOfNotNull(vararg items: T?): Array<T> = listOfNotNull(*items).toTypedArray()

data class RedisScanStepResult(val nextCursor: Long, val items: List<String>)

internal suspend fun Redis._scanBaseStep(
    cmd: String,
    key: String?,
    cursor: Long,
    pattern: String? = null,
    count: Int? = null
): RedisScanStepResult {
    val result = executeArrayAny(*arrayListOf<Any?>().apply {
        this += cmd
        if (key != null) {
            this += key
        }
        this += cursor
        if (pattern != null) {
            this += "PATTERN"
            this += pattern
        }
        if (count != null) {
            this += "COUNT"
            this += count
        }
    }.toTypedArray())
    return RedisScanStepResult(
        result[0].toString().toLong(),
        result[1] as List<String>
    )
}

internal suspend fun Redis._scanBase(
    cmd: String, key: String?,
    pattern: String? = null, count: Int? = null,
    pairs: Boolean = false
): ReceiveChannel<Any> = GlobalScope.produce(context, (count ?: 10) * 2) {
    var cursor = 0L
    do {
        val result = _scanBaseStep(cmd, key, cursor, pattern, count)
        cursor = result.nextCursor
        val items = result.items
        if (pairs) {
            for (n in 0 until items.size step 2) {
                send(items[n + 0] to items[n + 1])
            }
        } else {
            for (item in items) {
                send(item)
            }
        }
    } while (cursor > 0L)
}

internal suspend fun Redis._scanBaseString(
    cmd: String, key: String?, pattern: String? = null, count: Int? = null
): ReceiveChannel<String> = _scanBase(cmd, key, pattern, count, pairs = false) as Channel<String>

internal suspend fun Redis._scanBasePairs(
    cmd: String, key: String?, pattern: String? = null, count: Int? = null
): ReceiveChannel<Pair<String, String>> =
    _scanBase(cmd, key, pattern, count, pairs = true) as Channel<Pair<String, String>>
