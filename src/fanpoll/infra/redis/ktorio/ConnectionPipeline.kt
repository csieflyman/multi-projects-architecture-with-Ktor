package fanpoll.infra.redis.ktorio

import fanpoll.infra.redis.ktorio.protocol.readRedisMessage
import fanpoll.infra.redis.ktorio.protocol.writeRedisValue
import fanpoll.infra.redis.ktorio.utils.completeWith
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.close
import io.ktor.utils.io.core.BytePacketBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import java.io.Closeable
import java.nio.charset.Charset

internal class RedisRequest(val args: Any?, val result: CompletableDeferred<Any?>?)

private const val DEFAULT_PIPELINE_SIZE = 10

/**
 * Redis connection pipeline
 * https://redis.io/topics/pipelining
 */
internal class ConnectionPipeline(
    socket: Socket,
    private val requestQueue: Channel<RedisRequest>,
    private val password: String?,
    private val charset: Charset,
    pipelineSize: Int = DEFAULT_PIPELINE_SIZE,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) : Closeable {
    private val input = socket.openReadChannel()
    private val output = socket.openWriteChannel()

    val context: Job = GlobalScope.launch {

        try {
            password?.let { auth(it) }
        } catch (cause: Throwable) {

            requestQueue.consumeEach {
                it.result?.completeExceptionally(cause)
            }

            // COMPATIBILITY => modified
            if(cause is CancellationException)
                requestQueue.cancel(cause)
            throw cause
        }

        requestQueue.consumeEach { request ->
            if (request.result != null) {
                receiver.send(request.result)
            }

            if (request.args != null) {
                // COMPATIBILITY => modified
                output.writePacket(BytePacketBuilder().writeRedisValue(request.args, charset = charset).build())
                output.flush()
            }
        }
    }

    private val receiver = GlobalScope.actor<CompletableDeferred<Any?>>(
        dispatcher + context, capacity = pipelineSize
    ) {
        val decoder = charset.newDecoder()!!

        consumeEach { result ->
            completeWith(result) {
                input.readRedisMessage(decoder)
            }
        }
        // COMPATIBILITY => modified
        output.close()
        socket.close()
    }

    override fun close() {
        context.cancel()
    }

    private suspend fun auth(password: String) {
        // COMPATIBILITY => modified
        output.writePacket(BytePacketBuilder().writeRedisValue(listOf("AUTH", password), charset = charset).build())
        output.flush()

        input.readRedisMessage(charset.newDecoder())
    }
}
