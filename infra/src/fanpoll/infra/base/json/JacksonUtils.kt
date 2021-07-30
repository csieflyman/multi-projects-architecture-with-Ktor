/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */
package fanpoll.infra.base.json

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.i18n.Lang
import fanpoll.infra.base.response.ResponseCode
import fanpoll.infra.base.util.DateTimeUtils
import java.io.InputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.*
import java.util.*

object Jackson {

    // https://github.com/FasterXML/jackson-module-kotlin
    val mapper: ObjectMapper = jacksonObjectMapper()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .registerModule(
            SimpleModule()
                .addSerializer(UUID::class.java, UUIDSerializer)
                .addDeserializer(UUID::class.java, UUIDDeSerializer)
                .addSerializer(ZoneId::class.java, ZoneIdSerializer)
                .addDeserializer(ZoneId::class.java, ZoneIdDeSerializer)
                .addSerializer(Instant::class.java, InstantSerializer)
                .addDeserializer(Instant::class.java, InstantDeSerializer)
                .addSerializer(ZonedDateTime::class.java, ZonedDateTimeSerializer)
                .addDeserializer(ZonedDateTime::class.java, ZonedDateTimeDeSerializer)
                .addSerializer(LocalDateTime::class.java, LocalDateTimeSerializer)
                .addDeserializer(LocalDateTime::class.java, LocalDateTimeDeSerializer)
                .addSerializer(LocalDate::class.java, LocalDateSerializer)
                .addDeserializer(LocalDate::class.java, LocalDateDeSerializer)
                .addSerializer(LocalTime::class.java, LocalTimeSerializer)
                .addDeserializer(LocalTime::class.java, LocalTimeDeSerializer)
                .addSerializer(BigDecimal::class.java, BigDecimalSerializer)
                .addDeserializer(BigDecimal::class.java, BigDecimalDeSerializer)
                .addSerializer(Lang::class.java, LangSerializer)
                .addDeserializer(Lang::class.java, LangDeSerializer)
        )

    fun toJson(data: Any): JsonNode {
        return try {
            mapper.valueToTree(data)
        } catch (e: Exception) {
            throw InternalServerException(ResponseCode.DEV_ERROR, "fail to json serialize", e)
        }
    }

    fun toJsonString(data: Any): String {
        return stringify(toJson(data))
    }

    fun newObject(): ObjectNode {
        return mapper.createObjectNode()
    }

    fun newArray(): ArrayNode {
        return mapper.createArrayNode()
    }

    fun stringify(json: JsonNode): String {
        return mapper.writeValueAsString(json)
    }

    fun parse(src: String): JsonNode {
        return try {
            mapper.readTree(src)
        } catch (e: Throwable) {
            throw JsonException("fail to parse json", e)
        }
    }

    fun parse(src: InputStream): JsonNode {
        return try {
            mapper.readTree(src)
        } catch (e: Throwable) {
            throw JsonException("fail to parse json", e)
        }
    }

    fun parse(src: ByteArray): JsonNode {
        return try {
            mapper.readTree(src)
        } catch (e: Throwable) {
            throw JsonException("fail to parse json", e)
        }
    }

    private object LangSerializer : JsonSerializer<Lang>() {

        override fun serialize(value: Lang?, gen: JsonGenerator, serializers: SerializerProvider?) {
            value?.let { gen.writeString(value.code) }
        }
    }

    private object LangDeSerializer : JsonDeserializer<Lang>() {

        override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): Lang {
            return Lang(p.text)
        }
    }

    private object UUIDSerializer : JsonSerializer<UUID>() {

        override fun serialize(value: UUID?, gen: JsonGenerator, serializers: SerializerProvider?) {
            value?.let { gen.writeString(value.toString()) }
        }
    }

    private object UUIDDeSerializer : JsonDeserializer<UUID>() {

        override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): UUID {
            return UUID.fromString(p.text)
        }
    }

    private object ZoneIdSerializer : JsonSerializer<ZoneId>() {

        override fun serialize(value: ZoneId?, gen: JsonGenerator, serializers: SerializerProvider?) {
            value?.let { gen.writeString(value.id) }
        }
    }

    private object ZoneIdDeSerializer : JsonDeserializer<ZoneId>() {

        override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): ZoneId {
            return ZoneId.of(p.text)
        }
    }

    private object InstantSerializer : JsonSerializer<Instant>() {

        override fun serialize(value: Instant?, gen: JsonGenerator, serializers: SerializerProvider?) {
            value?.let { gen.writeString(DateTimeUtils.UTC_DATE_TIME_FORMATTER.format(value)) }
        }
    }

    private object InstantDeSerializer : JsonDeserializer<Instant>() {

        override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): Instant {
            return ZonedDateTime.parse(p.text, DateTimeUtils.UTC_DATE_TIME_FORMATTER).toInstant()
        }
    }

    private object ZonedDateTimeSerializer : JsonSerializer<ZonedDateTime>() {

        override fun serialize(value: ZonedDateTime?, gen: JsonGenerator, serializers: SerializerProvider?) {
            value?.let { gen.writeString(DateTimeUtils.UTC_DATE_TIME_FORMATTER.format(value)) }
        }
    }

    private object ZonedDateTimeDeSerializer : JsonDeserializer<ZonedDateTime>() {

        override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): ZonedDateTime {
            return ZonedDateTime.parse(p.text, DateTimeUtils.UTC_DATE_TIME_FORMATTER)
        }
    }

    private object LocalDateTimeSerializer : JsonSerializer<LocalDateTime>() {

        override fun serialize(value: LocalDateTime?, gen: JsonGenerator, serializers: SerializerProvider?) {
            value?.let { gen.writeString(DateTimeUtils.LOCAL_DATE_TIME_FORMATTER.format(value)) }
        }
    }

    private object LocalDateTimeDeSerializer : JsonDeserializer<LocalDateTime>() {

        override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): LocalDateTime {
            return LocalDateTime.parse(p.text, DateTimeUtils.LOCAL_DATE_TIME_FORMATTER)
        }
    }

    private object LocalDateSerializer : JsonSerializer<LocalDate>() {

        override fun serialize(value: LocalDate?, gen: JsonGenerator, serializers: SerializerProvider?) {
            value?.let { gen.writeString(DateTimeUtils.LOCAL_DATE_FORMATTER.format(value)) }
        }
    }

    private object LocalDateDeSerializer : JsonDeserializer<LocalDate>() {

        override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): LocalDate {
            return LocalDate.parse(p.text, DateTimeUtils.LOCAL_DATE_FORMATTER)
        }
    }

    private object LocalTimeSerializer : JsonSerializer<LocalTime>() {

        override fun serialize(value: LocalTime?, gen: JsonGenerator, serializers: SerializerProvider?) {
            value?.let { gen.writeString(DateTimeUtils.LOCAL_TIME_FORMATTER.format(value)) }
        }
    }

    private object LocalTimeDeSerializer : JsonDeserializer<LocalTime>() {

        override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): LocalTime {
            return LocalTime.parse(p.text, DateTimeUtils.LOCAL_TIME_FORMATTER)
        }
    }

    private const val DEFAULT_SCALE = 0
    private val DEFAULT_ROUNDING_MODE = RoundingMode.UP

    private object BigDecimalSerializer : JsonSerializer<BigDecimal>() {

        override fun serialize(value: BigDecimal?, gen: JsonGenerator, serializers: SerializerProvider?) {
            value?.let { gen.writeNumber(value.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE).toDouble()) }
        }

    }

    private object BigDecimalDeSerializer : JsonDeserializer<BigDecimal>() {

        override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): BigDecimal {
            return p.decimalValue.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE)
        }
    }
}

//fun Any.toJsonString(): String = mapper.writeValueAsString(this)
//
//fun Any.toJsonNode(): JsonNode = mapper.convertValue(this, JsonNode::class.java)
//
//fun String.toJsonNode(): JsonNode = mapper.readTree(this)
//
//fun String.jsonToMap(): Map<String, *> = mapper.readValue(this, object : TypeReference<Map<String, *>>() {})

//fun <T> String.jsonToObject(clazz: Class<T>): T = mapper.readValue(this, clazz)
//
//fun <T> String.jsonToList(): List<T> = mapper.readValue(this, object : TypeReference<List<T>>() {})

//fun <T> TreeNode.toObject(clazz: Class<T>): T = mapper.treeToValue(this, clazz)


