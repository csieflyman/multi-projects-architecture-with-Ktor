/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.base.json.kotlinx

import fanpoll.infra.base.extension.copyPropsFrom
import fanpoll.infra.base.json.JsonException
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.*
import java.util.*
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.typeOf

@OptIn(InternalSerializationApi::class)
fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is JsonElement -> this
    is String -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is Number -> this.toJsonElement()
    is UUID -> this.toJsonElement()
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
    is ByteArray -> JsonPrimitive(String(this, Charsets.UTF_8))
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
val JsonPrimitive.uuid: UUID get() = UUID.fromString(content)

val JsonPrimitive.uuidOrNull: UUID?
    get() = try {
        uuid
    } catch (e: Throwable) {
        null
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

fun JsonObject.path(path: String): JsonElement {
    return pathOrNull(path) ?: throw JsonException("JsonObject $path value is null", null, toString(), path)
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
                throw JsonException(
                    "JsonObject subPath ${
                        segments.subList(0, index).joinToString(".")
                    } does not exist", null, toString(), path
                )
            }
            val child = result!!.jsonObject[segment]
            if (child == null && index < segments.size - 1) {
                throw JsonException(
                    "JsonObject subPath ${
                        segments.subList(0, index).joinToString(".")
                    } value is null", null, toString(), path
                )
            }
            child
        } catch (e: IllegalArgumentException) {
            throw JsonException(
                "JsonObject subPath ${
                    segments.subList(0, index).joinToString(".")
                } is not JsonObject", e, toString(), path
            )
        }
        result = child
    }
    return result
}

fun JsonObjectBuilder.merge(element: JsonElement) {
    element.jsonObject.toMap().forEach { (key, value) -> put(key, value) }
}

private val typeOfJsonElement = typeOf<JsonElement>()

fun <T : Any> T.copyJsonPropsFrom(from: Any) {
    copyPropsFrom(
        from, true,
        javaClass.kotlin.memberProperties.filterNot {
            typeOfJsonElement.isSupertypeOf(it.returnType)
        }.map { it.name }
    )
}

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