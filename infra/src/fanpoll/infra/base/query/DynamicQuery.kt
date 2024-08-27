/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.query

import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.extension.myEquals
import fanpoll.infra.base.extension.myHashCode
import fanpoll.infra.base.json.kotlinx.json
import fanpoll.infra.base.response.InfraResponseCode
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.request.ApplicationRequest
import io.ktor.util.toMap
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus

@Serializable
class DynamicQuery(
    val fields: List<String>? = null,
    val filter: Predicate? = null,
    val orderByList: List<OrderBy>? = null,
    val offsetLimit: OffsetLimit? = null,
    val count: Boolean? = false,
    private val paramMap: MutableMap<String, String>? = null // q_xxx (unused now)
) {

    override fun toString(): String = listOfNotNull(
        fields?.let { "$QUERY_FIELDS=$it" },
        filter?.let {
            var dsl = it.toString()
            if (dsl.startsWith("("))
                dsl = dsl.substring(1, dsl.length - 1)
            "$QUERY_FILTER=[$dsl]"
        },
        orderByList?.joinToString("&"),
        offsetLimit?.toString(),
        count?.let { "$QUERY_COUNT=$it" },
        paramMap?.let { map -> map.entries.joinToString("&") { param -> "${param.key}=${param.value}" } },
    ).joinToString("&")

    companion object {

        private val logger = KotlinLogging.logger {}

        private const val QUERY_KEYWORD_PREFIX = "q_"

        private const val QUERY_FIELDS = "q_fields"
        private const val QUERY_FILTER = "q_filter"
        private const val QUERY_ORDER_BY = "q_orderBy"

        private const val QUERY_OFFSET = "q_offset" // start with 0
        private const val QUERY_LIMIT = "q_limit"
        private const val QUERY_PAGE_INDEX = "q_pageIndex" // start with 1
        private const val QUERY_ITEMS_PER_PAGE = "q_itemsPerPage"
        private const val MAX_LIMIT = 30

        private const val QUERY_COUNT = "q_count"

        private val queryKeywords: Set<String> = setOf(
            QUERY_FIELDS, QUERY_FILTER, QUERY_ORDER_BY,
            QUERY_OFFSET, QUERY_LIMIT,
            QUERY_ITEMS_PER_PAGE, QUERY_PAGE_INDEX,
            QUERY_COUNT
        )

        private val countQueryKeywords: Set<String> = setOf(QUERY_FILTER, QUERY_COUNT)

        fun from(location: DynamicQueryLocation): DynamicQuery = with(location) {
            DynamicQuery(
                q_fields?.split(","), q_filter?.let { parseFilter(it) }, q_orderBy?.let { parseOrderBy(it) },
                parseOffsetLimit(q_offset, q_limit, q_pageIndex, q_itemsPerPage),
                q_count, null
            )
        }

        fun from(form: DynamicQueryForm): DynamicQuery = with(form) {
            DynamicQuery(
                fields, filter?.let { parseFilter(it) }, orderBy?.let { parseOrderBy(it) },
                parseOffsetLimit(offset, limit, pageIndex, itemsPerPage),
                count, paramMap
            )
        }

        fun from(request: ApplicationRequest): DynamicQuery =
            from(request.queryParameters.toMap().mapValues { it.value[0] })

        fun from(text: String): DynamicQuery =
            from(text.split("&").associate { it.substringBefore("=") to it.substringAfter("=") })

        private fun from(map: Map<String, String>): DynamicQuery = DynamicQuery(
            map[QUERY_FIELDS]?.let { parseFields(it) },
            map[QUERY_FILTER]?.let { parseFilter(it) },
            map[QUERY_ORDER_BY]?.let { parseOrderBy(it) },
            parseOffsetLimit(
                map[QUERY_OFFSET]?.toLong(), map[QUERY_LIMIT]?.toInt(),
                map[QUERY_PAGE_INDEX]?.toLong(), map[QUERY_ITEMS_PER_PAGE]?.toInt()
            ),
            map[QUERY_COUNT]?.toBoolean()?.also { count ->
                if (count) {
                    val invalidKeywords = queryKeywords.filter { map.containsKey(it) }.subtract(countQueryKeywords)
                    if (invalidKeywords.isNotEmpty())
                        throw RequestException(
                            InfraResponseCode.BAD_REQUEST_QUERYSTRING,
                            "$QUERY_COUNT and $invalidKeywords are mutually exclusive"
                        )
                }
            },
            map.filter { it.key.startsWith(QUERY_KEYWORD_PREFIX) && !queryKeywords.contains(it.key) }.toMutableMap()
        )

        // Sort an array of strings lexicographically based on prefix
        val fieldComparator = Comparator<String> { s1, s2 ->
            if (s1.contains(s2) || s2.contains(s1))
                s1.length - s2.length
            else
                s1 compareTo s2
        }

        private fun parseFields(text: String): List<String> = text.split(",")
            .map { it.trim() }.toMutableSet().sortedWith(fieldComparator)

        // example: q=[a = 1 and b = 2 and (c = 3 or d = 4)]
        fun parseFilter(text: String): Predicate {
            var dsl = text.trim()
            logger.debug { "query filter: $dsl" }
            if (!dsl.startsWith("[") || !dsl.endsWith("]"))
                throw RequestException(
                    InfraResponseCode.BAD_REQUEST_QUERYSTRING,
                    "invalid query filter: filter should be startWith '[' and endWith ']"
                )
            dsl = dsl.substring(1, dsl.length - 1)
            val predicate = Predicate.valueOf(dsl.trim())
            logger.debug { "query filter parsed = ${predicate}" }
            return predicate
        }

        private fun parseOrderBy(text: String): List<OrderBy> = text.trim().split(",").map {
            val asc = if (it.endsWith('+') || it.endsWith('-')) it.last() == '+' else null
            val field = if (asc != null) it.substring(0, it.length - 1) else it
            OrderBy(field, asc)
        }

        private fun parseOffsetLimit(offset: Long?, limit: Int?, pageIndex: Long?, itemsPerPage: Int?): OffsetLimit? {
            validateOffsetLimit(offset, limit, pageIndex, itemsPerPage)
            return if (limit != null && offset != null)
                parseOffsetLimit(offset, limit)
            else if (pageIndex != null && itemsPerPage != null)
                parsePaging(pageIndex, itemsPerPage)
            else null
        }

        private fun parseOffsetLimit(offset: Long, limit: Int): OffsetLimit = try {
            if (offset < 0)
                throw RequestException(InfraResponseCode.BAD_REQUEST_QUERYSTRING, "$QUERY_OFFSET must be >= 0")
            if (limit < 1 || limit > MAX_LIMIT)
                throw RequestException(InfraResponseCode.BAD_REQUEST_QUERYSTRING, "$QUERY_LIMIT must be 1 ~ $MAX_LIMIT")

            OffsetLimit(offset, limit, false)
        } catch (e: NumberFormatException) {
            throw RequestException(InfraResponseCode.BAD_REQUEST_QUERYSTRING, "$QUERY_OFFSET/$QUERY_LIMIT must be integer", e)
        }

        private fun parsePaging(pageIndex: Long, itemsPerPage: Int): OffsetLimit = try {
            if (pageIndex < 1)
                throw RequestException(InfraResponseCode.BAD_REQUEST_QUERYSTRING, "$QUERY_PAGE_INDEX must be >= 1")
            if (itemsPerPage < 1 || itemsPerPage > MAX_LIMIT)
                throw RequestException(InfraResponseCode.BAD_REQUEST_QUERYSTRING, "$QUERY_ITEMS_PER_PAGE must be 1 ~ $MAX_LIMIT")

            val offset = (pageIndex - 1) * itemsPerPage
            OffsetLimit(offset, itemsPerPage, true)
        } catch (e: NumberFormatException) {
            throw RequestException(InfraResponseCode.BAD_REQUEST_QUERYSTRING, "$QUERY_PAGE_INDEX/$QUERY_ITEMS_PER_PAGE must be integer", e)
        }

        private fun validateOffsetLimit(offset: Long?, limit: Int?, pageIndex: Long?, itemsPerPage: Int?) {
            if ((offset != null).xor(limit != null))
                throw RequestException(InfraResponseCode.BAD_REQUEST_QUERYSTRING, "$QUERY_OFFSET and $QUERY_LIMIT are mutually necessary")

            if ((pageIndex != null).xor(itemsPerPage != null))
                throw RequestException(
                    InfraResponseCode.BAD_REQUEST_QUERYSTRING,
                    "$QUERY_PAGE_INDEX and $QUERY_ITEMS_PER_PAGE are mutually necessary"
                )

            if (offset != null && pageIndex != null)
                throw RequestException(
                    InfraResponseCode.BAD_REQUEST_QUERYSTRING,
                    "$QUERY_OFFSET/$QUERY_LIMIT and $QUERY_PAGE_INDEX/$QUERY_ITEMS_PER_PAGE are mutually exclusive"
                )
        }
    }

    fun getAllFields(): List<String> {
        val allFields = mutableSetOf<String>()
        if (fields != null)
            allFields.addAll(fields)
        if (filter != null)
            allFields.addAll(filter.getFields())
        if (orderByList != null)
            allFields.addAll(orderByList.map { it.field }.toList())
        return allFields.toList().sortedWith(fieldComparator)
    }

    @Serializable
    data class OrderBy(val field: String, val asc: Boolean? = true) {

        override fun equals(other: Any?) = myEquals(other, { field })
        override fun hashCode() = myHashCode({ field })

        override fun toString(): String = field + if (asc == true) "+" else "-"
    }

    @Serializable
    class OffsetLimit(val offset: Long, val limit: Int, val isPaging: Boolean) {

        init {
            require(offset >= 0)
            require(limit >= 1)
        }

        val pageIndex: Long
            get() {
                require(isPaging)
                return (offset / limit) + 1
            }

        val itemsPerPage: Int
            get() {
                require(isPaging)
                return limit
            }

        override fun toString(): String = if (isPaging)
            "$QUERY_ITEMS_PER_PAGE=$itemsPerPage&$QUERY_PAGE_INDEX=$pageIndex"
        else "$QUERY_OFFSET=$offset&$QUERY_LIMIT=$limit"
    }

    enum class PredicateValueKind {
        NONE, SINGLE, MULTIPLE
    }

    enum class PredicateOperator(
        private val queryStringExpr: String,
        val sqlExpr: String = queryStringExpr,
        val valueKind: PredicateValueKind = PredicateValueKind.SINGLE
    ) {

        EQ("="), NEQ("!="), LIKE("like"),
        GT(">"), GE(">="), LT("<"), LE("<="),
        IN("in", valueKind = PredicateValueKind.MULTIPLE),
        NOT_IN("not_in", "not in", PredicateValueKind.MULTIPLE),
        IS_NULL("is_null", "is null", PredicateValueKind.NONE),
        IS_NOT_NULL("is_not_null", "is not null", PredicateValueKind.NONE);

        companion object {

            fun queryStringValueOf(expr: String): PredicateOperator = try {
                entries.first { it.sqlExpr == expr }
            } catch (e: NoSuchElementException) {
                throw RequestException(InfraResponseCode.BAD_REQUEST_QUERYSTRING, "invalid query predicate operator: $expr")
            }
        }
    }

    @Serializable(Predicate.Companion::class)
    sealed class Predicate {

        companion object : KSerializer<Predicate> {

            init {
                json.serializersModule.plus(SerializersModule {
                    polymorphicDefaultSerializer(Predicate::class) { serializer() }
                })
            }

            private val NONE_VALUE_REGEX = """(\S+) (is_null|is_not_null)""".toRegex()
            private val SINGLE_VALUE_REGEX = """(\S+) ([=><]|!=|>=|<=|like) (\S+)""".toRegex()
            private val MULTIPLE_VALUE_REGEX = """(\S+) (in|not_in) \(([\S\s]+)\)""".toRegex()

            override val descriptor: SerialDescriptor =
                PrimitiveSerialDescriptor("fanpoll.infra.base.query.DynamicQuery.Predicate", PrimitiveKind.STRING)

            override fun deserialize(decoder: Decoder): Predicate {
                return valueOf(decoder.decodeString())
            }

            override fun serialize(encoder: Encoder, value: Predicate) {
                encoder.encodeString(value.toString())
            }

            fun valueOf(inputDsl: String): Predicate {
                val dsl = inputDsl.trim()

                // case Simple: a = 1
                if (!dsl.contains("and", true) && !dsl.contains("or", true)) {
                    return parseSimpleDsl(dsl)
                } else {
                    // case Junction: a = 1 or b = 2 or c = 3
                    val junctionDsl = dsl.split(" and ", " or ", ignoreCase = true).map { it.trim() }
                    if (junctionDsl.none { it.startsWith('(') }) {
                        val containAnd = dsl.contains("and", true)
                        val containOr = dsl.contains("or", true)
                        if (containAnd && containOr)
                            throw RequestException(
                                InfraResponseCode.BAD_REQUEST_QUERYSTRING,
                                "invalid query filter: operator should be either 'and' or 'or' => $dsl"
                            )
                        return Junction(containAnd, junctionDsl.map { parseSimpleDsl(it) }.toMutableList())
                    }

                    // case nested Junction: (a = 1) and (b = 2 or c = 3) and ((d = 4) or (e = 5 and f = 6))
                    //                         simple          junction           nested junction
                    // Note: a = 1, d = 4 => error!, predicate combined with junction should be enclosed in parentheses
                    val nestedJunctions = buildNestedJunctionIndexPairs(dsl)
                    var isConjunction = true
                    val predicates = nestedJunctions.mapIndexed { index, (startIndex, endIndex) ->
                        if (index < nestedJunctions.size - 1) {
                            val isAnd = dsl.substring(endIndex + 1, nestedJunctions[index + 1].first).trim() == "and"
                            if (index == 0)
                                isConjunction = isAnd
                            else if (isConjunction != isAnd) {
                                throw RequestException(
                                    InfraResponseCode.BAD_REQUEST_QUERYSTRING,
                                    "invalid query filter: operator should be either 'and' or 'or' => $dsl"
                                )
                            }
                        }
                        valueOf(dsl.substring(startIndex + 1, endIndex - 1))
                    }.toMutableList()
                    return Junction(isConjunction, predicates)
                }
            }

            private fun buildNestedJunctionIndexPairs(dsl: String): List<Pair<Int, Int>> {
                val junctionIndexPairs: List<Pair<Int, Int>> = mutableListOf()
                var depth = 0
                var startIndex = 0
                var endIndex = 0
                for ((index, element) in dsl.toCharArray().withIndex()) {
                    if (element == '(') {
                        if (depth == 0)
                            startIndex = index
                        depth++
                    } else if (element == ')') {
                        depth--
                        if (depth == 0)
                            endIndex = index
                        junctionIndexPairs + Pair(startIndex, endIndex)
                    }
                }
                if (depth != 0)
                    throw RequestException(
                        InfraResponseCode.BAD_REQUEST_QUERYSTRING,
                        "invalid query filter: missing '(' or ')' => $dsl"
                    )
                return junctionIndexPairs
            }

            private fun parseSimpleDsl(inputDsl: String): Predicate {
                val dsl = inputDsl.trim()
                val predicate = NONE_VALUE_REGEX.find(dsl)?.let {
                    val (field, operator) = it.destructured
                    return Simple(field, PredicateOperator.queryStringValueOf(operator), null)
                } ?: SINGLE_VALUE_REGEX.find(dsl)?.let {
                    val (field, operator, value) = it.destructured
                    return Simple(field, PredicateOperator.queryStringValueOf(operator), value)
                } ?: MULTIPLE_VALUE_REGEX.find(dsl)?.let {
                    val (field, operator, values) = it.destructured
                    return Simple(
                        field,
                        PredicateOperator.queryStringValueOf(operator),
                        values.trim().split(",").map { value -> value.trim() }.toMutableSet()
                    )
                }
                return predicate ?: throw RequestException(
                    InfraResponseCode.BAD_REQUEST_QUERYSTRING,
                    "invalid query filter: syntax error => $dsl"
                )
            }
        }

        data class Simple(val field: String, val operator: PredicateOperator, val value: Any?) : Predicate() {

            init {
                require(if (operator.valueKind == PredicateValueKind.NONE) value == null else true)
                require(if (operator.valueKind == PredicateValueKind.SINGLE) value !is Collection<*> else true)
                require(if (operator.valueKind == PredicateValueKind.MULTIPLE) value is Collection<*> else true)
            }

            override fun toString(): String = "$field ${operator.sqlExpr} " +
                    "${(value as? Iterable<*>)?.joinToString(separator = ",", prefix = "(", postfix = ")") ?: value ?: ""}"

            override fun add(predicate: Predicate): Predicate {
                return Junction(true, mutableListOf(this, predicate))
            }

            override fun equals(other: Any?) = myEquals(other, { field }, { operator })
            override fun hashCode() = myHashCode({ field }, { operator })
        }

        class Junction(val isConjunction: Boolean, val children: MutableList<Predicate>) : Predicate() {

            override fun toString(): String = children.joinToString(separator = if (isConjunction) " and " else " or ",
                prefix = "(", postfix = ")", transform = { child -> child.toString() })

            override fun add(predicate: Predicate): Predicate {
                children.add(predicate)
                return this
            }
        }

        // ASSUMPTION => suppose and operator now
        abstract fun add(predicate: Predicate): Predicate

        fun getFields(): List<String> {
            return when (this) {
                is Simple -> listOf(field)
                is Junction -> children.flatMap { it.getFields() }.toList()
            }
        }
    }

}
