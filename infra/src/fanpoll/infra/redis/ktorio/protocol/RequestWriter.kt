package fanpoll.infra.redis.ktorio.protocol

import io.ktor.utils.io.core.BytePacketBuilder
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeFully
import java.nio.charset.Charset

/**
 * TODO: Consider to remove duplicated data types
 */
// COMPATIBILITY => modified
internal fun BytePacketBuilder.writeRedisValue(
    value: Any?,
    forceBulk: Boolean = true, // TODO: consider use cases
    charset: Charset = Charsets.UTF_8
): BytePacketBuilder {
    when {
        value is List<*> -> writeListValue(value, forceBulk, charset)
        value is Array<*> -> writeArrayValue(value, forceBulk, charset)
        value is ByteArray -> writeByteArray(value)
        forceBulk -> writeBulk(value, charset)
        value is String -> writeString(value, charset)
        value is Int || value is Long -> writeIntegral(value)
        value == null -> writeNull()
        value is Throwable -> writeThrowable(value)
        else -> error("Unsupported $value to write")
    }
    return this
}

private fun <T : List<*>> BytePacketBuilder.writeListValue(
    value: T,
    forceBulk: Boolean = true, charset: Charset = Charsets.UTF_8
) {
    append(RedisType.ARRAY)
    append(value.size.toString())
    appendEOL()
    for (item in value) writeRedisValue(item, forceBulk, charset)
}

private fun BytePacketBuilder.writeArrayValue(
    value: Array<*>,
    forceBulk: Boolean = true, charset: Charset = Charsets.UTF_8
) {
    append(RedisType.ARRAY)
    append(value.size.toString())
    appendEOL()
    for (item in value) writeRedisValue(item, forceBulk, charset)
}

private fun BytePacketBuilder.writeBulk(value: Any?, charset: Charset) {
    val packet = buildPacket {
        writeStringEncoded(value.toString(), charset = charset)
    }
    append(RedisType.BULK)
    append(packet.remaining.toString())
    appendEOL()
    writePacket(packet)
    appendEOL()
}

private fun BytePacketBuilder.writeByteArray(value: ByteArray) {
    append(RedisType.BULK)
    append(value.size.toString())
    appendEOL()
    writeFully(value)
    appendEOL()
}

private fun BytePacketBuilder.writeString(value: String, charset: Charset) {
    if (value.contains('\n') || value.contains('\r')) {
        val packet = buildPacket { writeStringEncoded(value, charset) }
        append(RedisType.BULK)
        append(packet.remaining.toString())
        appendEOL()
        writePacket(packet)
        appendEOL()
        return
    }

    append('+')
    writeStringEncoded(value, charset)
    appendEOL()
}

private fun BytePacketBuilder.writeIntegral(value: Any) {
    append(RedisType.NUMBER)
    append(value.toString())
    appendEOL()
}

private fun BytePacketBuilder.writeNull() {
    append(RedisType.BULK)
    append("-1")
    appendEOL()
}

private fun BytePacketBuilder.writeThrowable(value: Throwable) {
    val message = (value.message ?: "Error")
        .replace("\r", "")
        .replace("\n", "")

    append(RedisType.ERROR)
    append(message)
    appendEOL()
}

private fun BytePacketBuilder.append(type: RedisType) {
    writeByte(type.code)
}

private fun BytePacketBuilder.appendEOL() {
    writeFully(EOL)
}

private fun BytePacketBuilder.writeStringEncoded(string: String, charset: Charset) {
    writeFully(charset.encode(string))
}

