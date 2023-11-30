package fanpoll.infra.redis.ktorio.protocol

internal val EOL = "\r\n".toByteArray()

internal enum class RedisType(val code: Byte) {
    STRING('+'.toByte()),
    NUMBER(':'.toByte()),
    BULK('$'.toByte()),
    ARRAY('*'.toByte()),
    ERROR('-'.toByte());

    companion object {
        fun fromCode(code: Byte): RedisType =
            types.find { it.code == code } ?: throw RedisException("No suitable message type found")

        val types: Array<RedisType> = entries.toTypedArray()
    }
}

class RedisException(message: String, args: Array<out Any?>? = null)
    : Exception(if (args == null) message else "$message processing '${args.getOrNull(0)?.toString()?.toUpperCase()}'")
