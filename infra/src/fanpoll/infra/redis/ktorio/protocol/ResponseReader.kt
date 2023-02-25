package fanpoll.infra.redis.ktorio.protocol

import fanpoll.infra.redis.ktorio.utils.RedisBufferPool
import fanpoll.infra.redis.ktorio.utils.RedisCharBufferPool
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.readBytes
import io.ktor.utils.io.pool.ObjectPool
import io.ktor.utils.io.readUntilDelimiter
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CharsetDecoder

internal suspend fun ByteReadChannel.readRedisMessage(
    decoder: CharsetDecoder = Charsets.UTF_8.newDecoder()
): Any? {
    val type = RedisType.fromCode(readByte())
    val line = readCRLFLine(decoder)
    return when (type) {
        RedisType.STRING -> line
        RedisType.ERROR -> throw RedisException(line)
        RedisType.NUMBER -> line.toLong()
        RedisType.BULK -> {
            val size = line.toInt()
            if (size < 0) return null
            val content = readPacket(size).readBytes()
            readShort() // Skip CRLF
            content
        }
        RedisType.ARRAY -> {
            val arraySize = line.toInt()
            (0 until arraySize).map { readRedisMessage(decoder) }
        }
    }
}

private suspend fun ByteReadChannel.readCRLFLine(
    decoder: CharsetDecoder,
    pool: ObjectPool<ByteBuffer> = RedisBufferPool,
    charPool: ObjectPool<CharBuffer> = RedisCharBufferPool
): String {
    val EOL = ByteBuffer.wrap(EOL)!!
    val result = StringBuilder()
    val buffer = pool.borrow()
    val charBuffer = charPool.borrow()

    decoder.reset()
    try {
        while (true) {
            buffer.clear()
            val count = readUntilDelimiter(EOL, buffer)
            buffer.flip()

            if (count <= 0) {
                if (count == 0) readShort() // CRLF

                charBuffer.clear()
                decoder.decode(buffer, charBuffer, true)
                charBuffer.flip()
                result.append(charBuffer)
                break
            }

            while (buffer.hasRemaining()) {
                charBuffer.clear()
                decoder.decode(buffer, charBuffer, false)
                charBuffer.flip()
                result.append(charBuffer)
            }
        }

        return result.toString()
    } finally {
        pool.recycle(buffer)
        charPool.recycle(charBuffer)
    }
}

