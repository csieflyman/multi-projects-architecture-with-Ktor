/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package fanpoll.infra.utils

import fanpoll.infra.JsonDataException
import fanpoll.infra.auth.UserRole
import fanpoll.infra.auth.UserType
import fanpoll.infra.notification.NotificationType
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import mu.KotlinLogging
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.*
import java.util.*
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.typeOf

private val logger = KotlinLogging.logger {}

/** use kotlinx.serialization 1.0 */
val json = Json {
    ignoreUnknownKeys = true
    serializersModule = SerializersModule {
        polymorphicDefault(UserType::class) { UserType.serializer() }
        polymorphicDefault(UserRole::class) { UserRole.serializer() }
        polymorphicDefault(DynamicQuery.Predicate::class) { DynamicQuery.Predicate.serializer() }
        polymorphicDefault(NotificationType::class) { NotificationType.serializer() }
    }
}

// ========== Any, Map Type Serialization ==========

@OptIn(InternalSerializationApi::class)
fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is JsonElement -> this
    is String -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is Number -> this.toJsonElement()
    is UUID -> this.toJsonElement()
    is BigDecimal -> this.toJsonElement()
    is Instant -> this.toJsonElement()
    is ZonedDateTime -> this.toJsonElement()
    is LocalDateTime -> this.toJsonElement()
    is LocalDate -> this.toJsonElement()
    is LocalTime -> this.toJsonElement()
    is ZoneId -> this.toJsonElement()
    is Iterable<*> -> JsonArray(this.map { it.toJsonElement() })
    is Array<*> -> JsonArray(this.map { it.toJsonElement() })
    is Map<*, *> -> this.toJsonObject()
    is Enum<*> -> JsonPrimitive(name)

    is CustomNameEnum<*> -> this.toJsonElement()

    else -> json.encodeToJsonElement(this.javaClass.kotlin.serializer(), this)
}

fun Any?.toJsonString(): String = this.toJsonElement().toString()

fun Map<*, *>.toJsonObject(): JsonObject =
    JsonObject(mapKeys { if (it.key is String) it.key as String else it.key.toString() }
        .mapValues { it.value.toJsonElement() })

// kotlinx.serialization 沒有 global serializer 的設定，所以只好透過 extension function，並定義自己的 CustomSerializer
fun Number.toJsonElement(): JsonElement = JsonPrimitive(this)
fun UUID.toJsonElement(): JsonElement = json.encodeToJsonElement(UUIDSerializer, this)
fun BigDecimal.toJsonElement(): JsonElement = json.encodeToJsonElement(BigDecimalSerializer, this)
fun Instant.toJsonElement(): JsonElement = json.encodeToJsonElement(InstantSerializer, this)
fun ZonedDateTime.toJsonElement(): JsonElement = json.encodeToJsonElement(ZonedDateTimeSerializer, this)
fun LocalDateTime.toJsonElement(): JsonElement = json.encodeToJsonElement(LocalDateTimeSerializer, this)
fun LocalDate.toJsonElement(): JsonElement = json.encodeToJsonElement(LocalDateSerializer, this)
fun LocalTime.toJsonElement(): JsonElement = json.encodeToJsonElement(LocalTimeSerializer, this)
fun ZoneId.toJsonElement(): JsonElement = json.encodeToJsonElement(ZoneIdSerializer, this)

// ========== Customized Type and Serializer ==========

val JsonPrimitive.uuid: UUID get() = UUID.fromString(content)

val JsonPrimitive.uuidOrNull: UUID?
    get() = try {
        uuid
    } catch (e: Throwable) {
        null
    }

@Serializer(forClass = UUID::class)
object UUIDSerializer : KSerializer<UUID> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("java.util.UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}

private const val DEFAULT_SCALE = 0
private val DEFAULT_ROUNDING_MODE = RoundingMode.UP

val JsonPrimitive.decimal: BigDecimal get() = content.toBigDecimal().setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE)

val JsonPrimitive.decimalOrNull: BigDecimal?
    get() = try {
        decimal
    } catch (e: Throwable) {
        null
    }

@Serializer(forClass = BigDecimal::class)
object BigDecimalSerializer : KSerializer<BigDecimal> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("java.math.BigDecimal", PrimitiveKind.DOUBLE)

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeDouble(value.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE).toDouble())
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        return BigDecimal(decoder.decodeDouble()).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE)
    }
}

@Serializer(forClass = Instant::class)
object InstantSerializer : KSerializer<Instant> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("java.time.Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(DateTimeUtils.UTC_DATE_TIME_FORMATTER.format(value))
    }

    override fun deserialize(decoder: Decoder): Instant {
        return ZonedDateTime.parse(decoder.decodeString(), DateTimeUtils.UTC_DATE_TIME_FORMATTER).toInstant()
    }
}

@Serializer(forClass = Instant::class)
object TaiwanInstantSerializer : KSerializer<Instant> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("java.time.Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(DateTimeUtils.TAIWAN_DATE_TIME_FORMATTER.format(value))
    }

    override fun deserialize(decoder: Decoder): Instant {
        return ZonedDateTime.parse(decoder.decodeString(), DateTimeUtils.TAIWAN_DATE_TIME_FORMATTER).toInstant()
    }
}

@Serializer(forClass = ZonedDateTime::class)
object ZonedDateTimeSerializer : KSerializer<ZonedDateTime> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.time.ZonedDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ZonedDateTime) {
        encoder.encodeString(DateTimeUtils.UTC_DATE_TIME_FORMATTER.format(value))
    }

    override fun deserialize(decoder: Decoder): ZonedDateTime {
        return ZonedDateTime.parse(decoder.decodeString(), DateTimeUtils.UTC_DATE_TIME_FORMATTER)
    }
}

@Serializer(forClass = LocalDateTime::class)
object LocalDateTimeSerializer : KSerializer<LocalDateTime> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.time.LocalDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(DateTimeUtils.LOCAL_DATE_TIME_FORMATTER.format(value))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString(), DateTimeUtils.LOCAL_DATE_TIME_FORMATTER)
    }
}

@Serializer(forClass = LocalDate::class)
object LocalDateSerializer : KSerializer<LocalDate> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("java.time.LocalDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(DateTimeUtils.LOCAL_DATE_FORMATTER.format(value))
    }

    override fun deserialize(decoder: Decoder): LocalDate {
        return LocalDate.parse(decoder.decodeString(), DateTimeUtils.LOCAL_DATE_FORMATTER)
    }
}

@Serializer(forClass = LocalTime::class)
object LocalTimeSerializer : KSerializer<LocalTime> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("java.time.LocalTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalTime) {
        encoder.encodeString(DateTimeUtils.LOCAL_TIME_FORMATTER.format(value))
    }

    override fun deserialize(decoder: Decoder): LocalTime {
        return LocalTime.parse(decoder.decodeString(), DateTimeUtils.LOCAL_TIME_FORMATTER)
    }
}

@Serializer(forClass = ZoneId::class)
object ZoneIdSerializer : KSerializer<ZoneId> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("java.time.ZoneId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ZoneId) {
        encoder.encodeString(value.id)
    }

    override fun deserialize(decoder: Decoder): ZoneId {
        return ZoneId.of(decoder.decodeString())
    }
}

fun JsonObject.path(path: String): JsonElement {
    return pathOrNull(path) ?: throw JsonDataException("JsonObject $path value is null", this, path)
}

fun JsonObject.pathOrDefault(path: String, defaultValue: JsonElement): JsonElement {
    return pathOrNull(path) ?: defaultValue
}

fun JsonObject.pathOrNull(path: String): JsonElement? {
    val segments = path.split(".")
    var result: JsonElement? = this

    segments.forEachIndexed { index, segment ->
        val child = try {
            if (!result!!.jsonObject.containsKey(segment)) {
                throw JsonDataException(
                    "JsonObject subPath ${
                        segments.subList(0, index).joinToString(".")
                    } is not exist", this, path
                )
            }
            val child = result!!.jsonObject[segment]
            if (child == null && index < segments.size - 1) {
                throw JsonDataException(
                    "JsonObject subPath ${
                        segments.subList(0, index).joinToString(".")
                    } value is null", this, path
                )
            }
            child
        } catch (e: IllegalArgumentException) {
            throw JsonDataException(
                "JsonObject subPath ${
                    segments.subList(0, index).joinToString(".")
                } is not JsonObject", this, path
            )
        }
        result = child
    }
    return result
}

private val typeOfJsonElement = typeOf<JsonElement>()

fun <T : Any> T.deepMergeJsonPropsFrom(from: Any) {
    javaClass.kotlin.memberProperties.filter { typeOfJsonElement.isSupertypeOf(it.returnType) }.forEach { prop ->
        val fromProp = from.javaClass.kotlin.memberProperties
            .firstOrNull { it.name == prop.name && typeOfJsonElement.isSupertypeOf(it.returnType) }
        if (fromProp != null) {
            (prop.get(this) as? JsonElement)?.deepMergeFrom(fromProp.get(from) as JsonElement?)
        }
    }
}

fun JsonElement?.deepMergeFrom(from: JsonElement?): JsonElement? {
    return when {
        from == null && this == null -> null
        from == null -> this
        this == null -> from
        else -> {
            if (this is JsonObject && from is JsonObject) {
                val fromObj = from.jsonObject
                val toObj = this.jsonObject
                val toMap: MutableMap<String, JsonElement> = toObj.toMutableMap()
                for ((prop, fromValue) in fromObj) {
                    if (!toObj.containsKey(prop)) {
                        toMap[prop] = fromValue
                    } else {
                        val toValue = toObj[prop]
                        if (fromValue is JsonObject && toValue is JsonObject) {
                            toMap[prop] = toValue.deepMergeFrom(fromValue)!!
                        } else {
                            toMap[prop] = fromValue
                        }
                    }
                }
                JsonObject(toMap.toMap())
            } else from
        }
    }
}

//private fun testDeepMergeFrom() {
//    val from = JsonObject(
//        mapOf(
//            "a" to JsonPrimitive(2),
//            "b" to JsonPrimitive(0),
//            "c" to JsonObject(
//                mapOf(
//                    "c1" to JsonPrimitive(3),
//                    "c2" to JsonPrimitive(4),
//                )
//            )
//        )
//    )
//    val to = JsonObject(
//        mapOf(
//            "a" to JsonPrimitive(1),
//            "c" to JsonObject(
//                mapOf(
//                    "c1" to JsonPrimitive(1),
//                    "c3" to JsonPrimitive(5)
//                )
//            ),
//            "d" to JsonPrimitive(1),
//        )
//    )
//    logger.debug { to.deepMergeFrom(from)?.toString() }
//}