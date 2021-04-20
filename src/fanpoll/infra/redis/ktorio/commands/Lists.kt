/**
 * Lists
 * Redis Lists are simply lists of strings, sorted by insertion order. It is possible to add elements to a Redis List
 * pushing new elements on the head (on the left) or on the tail (on the right) of the list.
 *
 * The LPUSH command inserts a new element on the head, while RPUSH inserts a new element on the tail.
 * A new list is created when one of this operations is performed against an empty key.
 * Similarly the key is removed from the key space if a list operation will empty the list.
 * These are very handy semantics since all the list commands will behave exactly like they were
 * called with an empty list if called with a non-existing key as argument.
 *
 * Some example of list operations and resulting lists:
 *
 * LPUSH mylist a   # now the list is "a"
 * LPUSH mylist b   # now the list is "b","a"
 * RPUSH mylist c   # now the list is "b","a","c" (RPUSH was used this time)
 * The max length of a list is 2**32 - 1 elements (4294967295, more than 4 billion of elements per list).
 *
 * The main features of Redis Lists from the point of view of time complexity are the support for constant time
 * insertion and deletion of elements near the head and tail, even with many millions of inserted items.
 * Accessing elements is very fast near the extremes of the list but is slow if you try accessing the middle
 * of a very big list, as it is an O(N) operation.
 *
 * You can do many interesting things with Redis Lists, for instance you can:
 *
 * Model a timeline in a social network, using LPUSH in order to add new elements in the user time line,
 * and using LRANGE in order to retrieve a few of recently inserted items.
 * You can use LPUSH together with LTRIM to create a list that never exceeds a given number of elements,
 * but just remembers the latest N elements.
 * Lists can be used as a message passing primitive, See for instance the well known Resque Ruby library
 * for creating background jobs.
 * You can do a lot more with lists, this data type supports a number of commands,
 * including blocking commands like BLPOP.
 * Please check all the available commands operating on lists for more information,
 * or read the introduction to Redis data types.
 */

package fanpoll.infra.redis.ktorio.commands

import fanpoll.infra.redis.ktorio.Redis

/**
 * Remove and get the first element in a list, or block until one is available
 *
 * https://redis.io/commands/blpop
 *
 * @since 2.0.0
 */
suspend fun Redis.blpop(vararg keys: String, timeout: Int = 0): Pair<String, String>? {
    val result = executeArrayString("BLPOP", *keys, timeout)
    return if (result.size >= 2) result[0] to result[1] else null
}

/**
 * Remove and get the last element in a list, or block until one is available
 *
 * https://redis.io/commands/brpop
 *
 * @since 2.0.0
 */
suspend fun Redis.brpop(vararg keys: String, timeout: Int = 0): Pair<String, String>? {
    val result = executeArrayString("BRPOP", *keys, timeout)
    return if (result.size >= 2) result[0] to result[1] else null
}

/**
 * Pop a value from a list, push it to another list and return it; or block until one is available
 *
 * https://redis.io/commands/brpoplpush
 *
 * @since 2.2.0
 */
suspend fun Redis.brpoplpush(src: String, dst: String, timeout: Int = 0): String? {
    val result = executeText("BRPOPLPUSH", src, dst, timeout)
    return if (result != listOf<Any?>()) result?.toString() else null
}

/**
 * Remove the last element in a list, prepend it to another list and return it
 *
 * https://redis.io/commands/rpoplpush
 *
 * @since 1.2.0
 */
suspend fun Redis.rpoplpush(src: String, dst: String): String? = executeTypedNull<String>("RPOPLPUSH", src, dst)

/**
 * Get an element from a list by its index
 *
 * https://redis.io/commands/lindex
 *
 * @since 1.0.0
 */
suspend fun Redis.lindex(key: String, index: Int): String? = executeTypedNull<String>("LINDEX", key, index)

/**
 * Insert an element before another element in a list
 *
 * https://redis.io/commands/linsert
 *
 * @since 2.2.0
 */
suspend fun Redis.linsertBefore(key: String, pivot: String, value: Any?): Long = executeTyped(
    "LINSERT",
    key,
    "BEFORE",
    pivot,
    value
)

/**
 * Insert an element after another element in a list
 *
 * https://redis.io/commands/linsert
 *
 * @since 2.2.0
 */
suspend fun Redis.linsertAfter(key: String, pivot: String, value: Any?): Long = executeTyped(
    "LINSERT",
    key,
    "AFTER",
    pivot,
    value
)

/**
 * Get the length of a list
 *
 * https://redis.io/commands/llen
 *
 * @since 1.0.0
 */
suspend fun Redis.llen(key: String): Long = executeTyped("LLEN", key)

/**
 * Remove and get the first element in a list
 *
 * https://redis.io/commands/lpop
 *
 * @since 1.0.0
 */
suspend fun Redis.lpop(key: String): String? = executeTypedNull<String>("LPOP", key)

/**
 * Remove and get the last element in a list
 *
 * https://redis.io/commands/rpop
 *
 * @since 1.0.0
 */
suspend fun Redis.rpop(key: String): String? = executeTypedNull<String>("RPOP", key)

/**
 * Insert all the specified values at the head of the list stored at key.
 *
 * https://redis.io/commands/lpush
 *
 * @since 1.0.0
 */
suspend fun Redis.lpush(key: String, value: String, vararg extraValues: String): Long = executeTyped(
    "LPUSH",
    key,
    value,
    *extraValues
)

/**
 * Prepend a value to a list, only if the list exists
 *
 * https://redis.io/commands/lpushx
 *
 * @since 2.2.0
 */
suspend fun Redis.lpushx(key: String, value: String): Long = executeTyped("LPUSHX", key, value)

/**
 * Append one or multiple values to a list
 *
 * https://redis.io/commands/rpush
 *
 * @since 1.0.0
 */
suspend fun Redis.rpush(key: String, value: String, vararg extraValues: String): Long = executeTyped(
    "RPUSH",
    key,
    value,
    *extraValues
)

/**
 * Append a value to a list, only if the list exists
 *
 * https://redis.io/commands/rpushx
 *
 * @since 2.2.0
 */
suspend fun Redis.rpushx(key: String, value: String): Long = executeTyped("RPUSHX", key, value)

/**
 * Get a range of elements from a list
 *
 * https://redis.io/commands/lrange
 *
 * @since 1.0.0
 */
suspend fun Redis.lrange(key: String, range: LongRange): List<String> = executeArrayString("LRANGE", key, range.start, range.endInclusive)

/**
 * Returns the whole list. (Shortcut of lrange(key, 0L until 4294967295L))
 */
suspend fun Redis.lgetall(key: String): List<String> = lrange(key, 0L until 4294967295L)

/**
 * Remove elements from a list
 *
 * https://redis.io/commands/lrem
 *
 * @since 1.0.0
 */
suspend fun Redis.lrem(key: String, count: Long, value: String): Long = executeTyped("LREM", key, count, value)

/**
 * Set the value of an element in a list by its index
 *
 * https://redis.io/commands/lset
 *
 * @since 1.0.0
 */
suspend fun Redis.lset(key: String, index: Long, value: String) = executeTyped<Unit>("LSET", key, index, value)

/**
 * Trim a list to the specified range
 *
 * https://redis.io/commands/ltrim
 *
 * @since 1.0.0
 */
suspend fun Redis.ltrim(key: String, range: LongRange) = executeTyped<Unit>("LTRIM", key, range.start, range.endInclusive)
