@file:OptIn(ExperimentalCoroutinesApi::class)

package fanpoll.infra.redis.ktorio.commands

import fanpoll.infra.base.util.IdentifiableObject
import fanpoll.infra.redis.ktorio.Redis
import fanpoll.infra.redis.ktorio.RedisInternalChannel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce

interface RedisPubSub {

    interface Packet

    data class Message(val channel: String, val message: String, val isPattern: Boolean = false) : Packet

    data class Subscription(val channel: String, val subscriptions: Long, val subscribe: Boolean, val isPattern: Boolean = false) : Packet

    data class KeyspaceNotification(val channel: String, val isKeyEvent: Boolean, val event: String, val key: String) :
        IdentifiableObject<String>(), Packet {

        override val id: String
            get() = "$event-$key"
    }

    //data class Packet(val channel: String, val content: String, val isPattern: Boolean, val isMessage: Boolean)
}

interface RedisPubSubInternal : RedisPubSub {
    val redis: Redis
}

internal class RedisPubSubImpl(override val redis: Redis) : RedisPubSubInternal {

    val rawChannel = redis.run { RedisInternalChannel.run { getMessageChannel() } }

}

fun CoroutineScope.redisMessageChannel(redisPubSub: RedisPubSub): ReceiveChannel<RedisPubSub.Message> =
    filterMessage(mapToPacket(((redisPubSub as RedisPubSubImpl).rawChannel)))

fun CoroutineScope.redisSubscriptionChannel(redisPubSub: RedisPubSub): ReceiveChannel<RedisPubSub.Subscription> =
    filterSubscription(mapToPacket(((redisPubSub as RedisPubSubImpl).rawChannel)))

fun CoroutineScope.redisKeyspaceNotificationChannel(redisPubSub: RedisPubSub): ReceiveChannel<RedisPubSub.KeyspaceNotification> =
    filterKeyspaceNotification(mapToPacket(((redisPubSub as RedisPubSubImpl).rawChannel)))

private fun CoroutineScope.filterMessage(channel: ReceiveChannel<RedisPubSub.Packet>) = produce {
    for (packet in channel) {
        if (packet is RedisPubSub.Message)
            send(packet)
    }
}

private fun CoroutineScope.filterSubscription(channel: ReceiveChannel<RedisPubSub.Packet>) = produce {
    for (packet in channel) {
        if (packet is RedisPubSub.Subscription)
            send(packet)
    }
}

private fun CoroutineScope.filterKeyspaceNotification(channel: ReceiveChannel<RedisPubSub.Packet>) = produce {
    for (packet in channel) {
        if (packet is RedisPubSub.KeyspaceNotification)
            send(packet)
    }
}

private val logger = KotlinLogging.logger {}

private fun CoroutineScope.mapToPacket(rawChannel: ReceiveChannel<Any>) = produce(capacity = Channel.UNLIMITED) {
    for (data in rawChannel) {
        logger.debug("data = $data")
        val list = data as List<Any>
        val kind = String(list[0] as ByteArray)
        val channel = String(list[1] as ByteArray)

        val isPattern = kind.startsWith("p")
        val isMessage = kind == "message"
        val isSubscription = kind.startsWith("psub") || kind.startsWith("sub")
        val isPMessage = kind == "pmessage"
        val isKeyspaceNotification = isPMessage && list.size == 4

        val packet = when {
            isMessage -> RedisPubSub.Message(channel, String(list[2] as ByteArray), isPattern)
            isSubscription -> RedisPubSub.Subscription(channel, list[2] as Long, isSubscription, isPattern)
            isKeyspaceNotification -> {
                val info = String(list[2] as ByteArray)
                val pMessage = String(list[3] as ByteArray)
                val isKeyEvent = info.contains("keyevent")
                val event = if (isKeyEvent) info.substringAfterLast(":") else pMessage
                val key = if (isKeyEvent) pMessage else info.substringAfterLast(":")
                RedisPubSub.KeyspaceNotification(channel, isKeyEvent, event, key)
            }
            else -> error("Undefined Redis PubSub raw data: $list")
        }
        logger.debug("packet = $packet")
        send(packet)
    }
}

/**
 * Starts a new pubsub session.
 */
private suspend fun Redis._pubsub(): RedisPubSub = RedisPubSubImpl(this)

/**
 * Listen for messages published to channels matching the given patterns
 *
 * https://redis.io/commands/psubscribe
 *
 * @since 2.0.0
 */
internal suspend fun Redis.psubscribe(vararg patterns: String): RedisPubSub = _pubsub().psubscribe(*patterns)

/**
 * Listen for messages published to the given channels
 *
 * https://redis.io/commands/subscribe
 *
 * @since 2.0.0
 */
internal suspend fun Redis.subscribe(vararg channels: String): RedisPubSub = _pubsub().subscribe(*channels)

/**
 * Listen for messages published to channels matching the given patterns
 *
 * https://redis.io/commands/psubscribe
 *
 * @since 2.0.0
 */
internal suspend fun RedisPubSub.psubscribe(vararg patterns: String): RedisPubSub =
    this.apply { (this as RedisPubSubImpl).redis.executeTyped("PSUBSCRIBE", *patterns) }

/**
 * Listen for messages published to the given channels
 *
 * https://redis.io/commands/subscribe
 *
 * @since 2.0.0
 */
internal suspend fun RedisPubSub.subscribe(vararg channels: String): RedisPubSub =
    this.apply { (this as RedisPubSubImpl).redis.executeTyped("SUBSCRIBE", *channels) }

/**
 * Stop listening for messages posted to channels matching the given patterns
 *
 * https://redis.io/commands/punsubscribe
 *
 * @since 2.0.0
 */
internal suspend fun RedisPubSub.punsubscribe(vararg patterns: String): RedisPubSub =
    this.apply { (this as RedisPubSubImpl).redis.executeTyped("PUNSUBSCRIBE", *patterns) }

/**
 * Stop listening for messages posted to the given channels
 *
 * https://redis.io/commands/unsubscribe
 *
 * @since 2.0.0
 */
internal suspend fun RedisPubSub.unsubscribe(vararg channels: String): RedisPubSub =
    this.apply { (this as RedisPubSubImpl).redis.executeTyped("UNSUBSCRIBE", *channels) }

/**
 * Post a message to a channel
 *
 * https://redis.io/commands/publish
 *
 * @since 2.0.0
 */
internal suspend fun Redis.publish(channel: String, message: String): Long =
    (this as RedisPubSubInternal).redis.executeTyped("PUBLISH", channel, message)

/**
 * Lists the currently active channels.
 * An active channel is a Pub/Sub channel with one or more subscribers (not including clients subscribed to patterns).
 * If no pattern is specified, all the channels are listed, otherwise if pattern is specified only channels matching
 * the specified glob-style pattern are listed.
 *
 * https://redis.io/commands/pubsub#pubsub-channels-pattern
 *
 * @since 2.8.0
 */
internal suspend fun RedisPubSub.pubsubChannels(pattern: String?): List<String> =
    (this as RedisPubSubInternal).redis.executeArrayString(
        *arrayOfNotNull(
            "PUBSUB",
            "CHANNELS",
            pattern
        )
    )

/**
 * Returns the number of subscribers (not counting clients subscribed to patterns) for the specified channels.
 *
 * https://redis.io/commands/pubsub#codepubsub-numsub-channel-1--channel-ncode
 *
 * @since 2.8.0
 */
internal suspend fun RedisPubSub.pubsubNumsub(vararg channels: String): Map<String, Long> =
    (this as RedisPubSubInternal).redis.executeArrayString("PUBSUB", "NUMSUB", *channels).toListOfPairsString()
        .map { it.first to it.second.toLong() }.toMap()

/**
 * Returns the number of subscriptions to patterns (that are performed using the PSUBSCRIBE command).
 * Note that this is not just the count of clients subscribed to patterns but the total number of patterns
 * all the clients are subscribed to.
 *
 * https://redis.io/commands/pubsub#codepubsub-numpatcode
 *
 * @since 2.8.0
 */
internal suspend fun RedisPubSub.pubsubNumpat(): Long =
    (this as RedisPubSubInternal).redis.executeTyped("PUBSUB", "NUMPAT")