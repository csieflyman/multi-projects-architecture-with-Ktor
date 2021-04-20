/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */
package fanpoll.infra.openapi.definition

import fanpoll.infra.*
import fanpoll.infra.app.AppVersion
import fanpoll.infra.auth.ClientVersionCheckResult
import fanpoll.infra.auth.HEADER_CLIENT_VERSION
import fanpoll.infra.auth.HEADER_CLIENT_VERSION_CHECK_RESULT
import fanpoll.infra.auth.RunAsAuth.RUN_AS_TOKEN_HEADER_NAME
import fanpoll.infra.auth.RunAsAuthProviderConfig
import fanpoll.infra.openapi.OpenApiConfig
import fanpoll.infra.openapi.definition.ComponentsUtils.createDataResponse
import fanpoll.infra.utils.I18nUtils
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode

interface ReusableComponents {

    fun loadReferenceObjects(): List<ReferenceObject>
}

object DefaultReusableComponents : ReusableComponents {

    // ==================== Schemas ====================

    val ResponseCodeValueSchema = PropertyDef("code", SchemaDataType.string, "4-digit number")

    private val ResponseCodeTypeSchema = PropertyDef(
        "codeType", SchemaDataType.string,
        ResponseCodeType.values().joinToString(" ; ") {
            "${it.name} => ${I18nUtils.getCodeTypeMessage(it, OpenApiConfig.langCode)}"
        },
        enum = ResponseCodeType.values().toList(),
    )

    private val ResponseMessageTypeSchema = PropertyDef(
        "messageType", SchemaDataType.string, enum = ResponseMessageType.values().toList()
    )

    private val ResponseCodeSchema = buildResponseCodeSchema()

    private fun buildResponseCodeSchema(): ModelDef {
        val requiredProperties = listOf("code", "codeType", "messageType", "httpStatusCode")
        val properties = listOf(
            ResponseCodeValueSchema,
            ResponseCodeTypeSchema,
            ResponseMessageTypeSchema,
            PropertyDef("httpStatusCode", SchemaDataType.integer)
        ).map { it.valuePair() }.toMap()
        return ModelDef(
            ResponseCode::class.simpleName!!, requiredProperties, properties,
            ComponentsUtils.buildResponseCodesDescription(ResponseCode.values().toList())
        ).also {
            it.properties.values.forEach { property -> (property.definition as SchemaDef).parent = it }
        }
    }

    private val ErrorResponseErrorsSchema = buildErrorResponseErrorsSchema()

    private fun buildErrorResponseErrorsSchema(): ArrayModelDef {
        val requiredProperties = listOf("code", "codeType", "detail")
        val properties: Map<String, Schema> = listOf(
            ResponseCodeValueSchema,
            ResponseCodeTypeSchema,
            PropertyDef("detail", SchemaDataType.string, "detail message for developer"),
            DictionaryPropertyDef("data", description = "JsonObject or JsonArray")
        ).map { it.valuePair() }.toMap()

        val modelDef = ModelDef(
            ErrorResponseDetailError::class.simpleName!!, requiredProperties, properties,
            kClass = ErrorResponseDetailError::class
        ).also {
            it.properties.values.forEach { property -> (property.definition as SchemaDef).parent = it }
        }
        val arrayModelDef = ArrayModelDef(
            "${ErrorResponseDetailError::class.simpleName!!}Array", modelDef,
            "all detail errors", "errors"
        )
        arrayModelDef.createRef()
        return arrayModelDef
    }

    val ErrorResponseSchema = buildErrorResponseSchema()

    private fun buildErrorResponseSchema(): ModelDef {
        val requiredProperties = listOf("code", "codeType", "messageType", "message", "detail", "reqId")
        val properties: Map<String, Schema> = listOf(
            ResponseCodeValueSchema,
            ResponseCodeTypeSchema,
            ResponseMessageTypeSchema,
            PropertyDef("message", SchemaDataType.string, "message for user"),
            PropertyDef("detail", SchemaDataType.string, "detail message for developer"),
            PropertyDef("reqId", SchemaDataType.string, "request unique id"),
            DictionaryPropertyDef("data", description = "JsonObject or JsonArray"),
            ErrorResponseErrorsSchema
        ).map { it.valuePair() }.toMap()

        return ModelDef(ErrorResponse::class.simpleName!!, requiredProperties, properties, kClass = ErrorResponse::class).also {
            it.properties.values.forEach { property -> (property.definition as SchemaDef).parent = it }
        }
    }

    private val DynamicQueryPagingResponseSchema =
        buildDynamicQueryPagingResponseSchema(DictionaryPropertyDef("default", description = "JsonObject of JsonArray"))

    fun buildDynamicQueryPagingResponseSchema(itemSchema: Schema): ModelDef {
        val requiredProperties = listOf("code", "data")
        val properties: Map<String, Schema> = listOf(
            ResponseCodeValueSchema,
            ModelDef(
                "data", listOf("total", "totalPages", "itemsPerPage", "pageIndex", "items"),
                listOf(
                    PropertyDef("total", SchemaDataType.integer),
                    PropertyDef("totalPages", SchemaDataType.integer),
                    PropertyDef("itemsPerPage", SchemaDataType.integer),
                    PropertyDef("pageIndex", SchemaDataType.integer),
                    ArrayModelDef("items", itemSchema),
                ).map { it.valuePair() }.toMap()
            ).also {
                it.properties.values.forEach { property -> (property.definition as SchemaDef).parent = it }
            }
        ).map { it.valuePair() }.toMap()

        return ModelDef("DynamicQueryPagingResponse-${itemSchema.name}", requiredProperties, properties).also {
            it.properties.values.forEach { property -> (property.definition as SchemaDef).parent = it }
        }
    }

    private val DynamicQueryItemsResponseSchema =
        buildDynamicQueryItemsResponseSchema(DictionaryPropertyDef("default", description = "JsonObject of JsonArray"))

    fun buildDynamicQueryItemsResponseSchema(itemSchema: Schema): ModelDef {
        val requiredProperties = listOf("code", "data")
        val properties: Map<String, Schema> = listOf(
            ResponseCodeValueSchema,
            ArrayModelDef("data", itemSchema),
        ).map { it.valuePair() }.toMap()

        return ModelDef("DynamicQueryItemsResponse-${itemSchema.name}", requiredProperties, properties).also {
            it.properties.values.forEach { property -> (property.definition as SchemaDef).parent = it }
        }
    }

    val DynamicQueryTotalResponseSchema = buildDynamicQueryTotalResponseSchema()

    private fun buildDynamicQueryTotalResponseSchema(): ModelDef {
        val requiredProperties = listOf("code", "data")
        val properties: Map<String, Schema> = listOf(
            ResponseCodeValueSchema,
            ModelDef(
                "data", listOf("total"),
                mapOf(PropertyDef("total", SchemaDataType.integer).valuePair())
            ),
        ).map { it.valuePair() }.toMap()

        return ModelDef("DynamicQueryTotalResponse", requiredProperties, properties).also {
            it.properties.values.forEach { property -> (property.definition as SchemaDef).parent = it }
        }
    }

    private val DynamicQueryResponseSchemas = OneOfSchema(
        "default",
        listOf(DynamicQueryPagingResponseSchema, DynamicQueryItemsResponseSchema, DynamicQueryTotalResponseSchema).map { it.createRef() }
    )

    // ==================== Parameter Schema ====================

    private val runAsTokenSchema = PropertyDef(
        RUN_AS_TOKEN_HEADER_NAME, SchemaDataType.string,
        description = RunAsAuthProviderConfig.tokenPatternDescription
    )

    private val ClientVersionSchema = PropertyDef(
        HEADER_CLIENT_VERSION, SchemaDataType.string,
        pattern = AppVersion.NAME_PATTERN, example = "1.0.0",
        description = "App 端須帶入程式版本號，Server 會回傳驗證結果至 response header => $HEADER_CLIENT_VERSION_CHECK_RESULT = ${
            ClientVersionCheckResult.values().toList()
        }"
    )

    private val ClientVersionCheckResultSchema = PropertyDef(
        HEADER_CLIENT_VERSION_CHECK_RESULT, SchemaDataType.string,
        enum = ClientVersionCheckResult.values().toList(), kClass = ClientVersionCheckResult::class
    )

    private val schemaList: List<ReferenceObject> = listOf(
        ResponseCodeSchema, ErrorResponseSchema, ErrorResponseErrorsSchema,
        DynamicQueryPagingResponseSchema, DynamicQueryItemsResponseSchema, DynamicQueryTotalResponseSchema,
        ClientVersionSchema, ClientVersionCheckResultSchema
    ).map { it.createRef() }

    // ==================== Headers ====================

    private val ClientVersionCheckResultResponseHeader = HeaderDef(
        true, ClientVersionCheckResultSchema
    ).createRef()

    private val headerList: List<ReferenceObject> = listOf(
        ClientVersionCheckResultResponseHeader
    )

    // ==================== Parameters ====================

    // ===== Parameters(Header) ======

    private val RunAsOptionalHeader = ParameterDef(ParameterInputType.header, false, runAsTokenSchema).createRef()

    val ClientVersionOptionalHeader = ParameterDef(ParameterInputType.header, false, ClientVersionSchema).createRef()

    private val headerParameterList: List<ReferenceObject> = listOf(
        RunAsOptionalHeader, ClientVersionOptionalHeader
    )

    // ===== Parameters(Query) ======

    val DynamicQueryParameters: List<ReferenceObject> = listOf(
        ParameterDef(
            ParameterInputType.query, false,
            PropertyDef("q_fields", SchemaDataType.string),
            "回傳欄位 => 可指定多個欄位，以逗號分隔。如果不指定則回傳所有欄位。"
        ).createRef(),
        ParameterDef(
            ParameterInputType.query, false, PropertyDef("q_filter", SchemaDataType.string),
            "查詢條件 DSL => 以 '[' 字元開始， ']' 字元結束，" +
                    "每個運算式 expression 的格式是 field operator value ，中間以空白字元分隔，多個條件以 and 或 or 連接 (目前不支援巢狀條件)。" +
                    "operator 支援 = != > >= < <= like in not_in is_null is_not_null 。" +
                    "範例: [name = fanpoll and price > 100 and type in (cpu,memory)] "
        ).createRef(),
        ParameterDef(
            ParameterInputType.query, false, PropertyDef("q_orderBy", SchemaDataType.string),
            "指定排序欄位 => 可指定多個欄位，以逗號分隔, 例如 name,price- 代表先以 name 欄位 asc 排序，再以 price 欄位 desc 排序 (+ 號代表 asc 預設可省略, - 號代表 desc)",
        ).createRef(),
        ParameterDef(
            ParameterInputType.query, false, PropertyDef("q_offset", SchemaDataType.integer),
            "限制回傳筆數 => 需一併指定 q_limit，從第 q_offset 筆(start from 0)，回傳 q_limit 筆資料"
        ).createRef(),
        ParameterDef(
            ParameterInputType.query, false, PropertyDef("q_limit", SchemaDataType.integer),
            "限制回傳筆數 => 需一併指定 q_offset，從第 q_offset 筆(start from 0)，回傳 q_limit 筆資料"
        ).createRef(),
        ParameterDef(
            ParameterInputType.query, false, PropertyDef("q_pageIndex", SchemaDataType.integer),
            "分頁查詢 => 需一併指定 q_itemsPerPage，回傳第 q_pageIndex 頁，每頁 q_itemsPerPage 筆"
        ).createRef(),
        ParameterDef(
            ParameterInputType.query, false, PropertyDef("q_itemsPerPage", SchemaDataType.integer),
            "分頁查詢 => 需一併指定 q_pageIndex，回傳第 q_pageIndex 頁，每頁 q_itemsPerPage 筆"
        ).createRef(),
        ParameterDef(
            ParameterInputType.query, false, PropertyDef("q_count", SchemaDataType.boolean),
            "僅回傳資料筆數 (預設值 false)"
        ).createRef()
    )

    private val parameterList: List<ReferenceObject> = listOf(headerParameterList, DynamicQueryParameters).flatten()

    // ==================== RequestBodies ====================

    private val requestBodiesList: List<ReferenceObject> = listOf()

    // ==================== Responses ====================

    val EmptyBodyResponse = ResponseDef(
        "EmptyBodyResponse", "Success", HttpStatusCode.OK
    ).createRef()

    val StringIdResponse = createDataResponse(
        ModelDef(
            "StringId", listOf("id"),
            mapOf("id" to PropertyDef("id", SchemaDataType.string)),
            "object created and return object string id"
        )
    ).createRef()

    val UUIDResponse = createDataResponse(
        ModelDef(
            "UUID", listOf("id"),
            mapOf("id" to PropertyDef("id", SchemaDataType.string, format = "uuid")),
            "object created and return object uuid"
        )
    ).createRef()

    val LongIdResponse = createDataResponse(
        ModelDef(
            "LongId", listOf("id"),
            mapOf("id" to PropertyDef("id", SchemaDataType.integer)),
            "object created and return object integer id"
        )
    ).createRef()

    val FreeFormDataResponse = createDataResponse(
        DictionaryPropertyDef("FreeForm", description = "JsonObject or JsonArray")
    ).createRef()

    fun buildDynamicQueryResponse(oneOfSchema: OneOfSchema): ResponseDef {
        return ResponseDef(
            "DynamicQueryPagingResponse-${oneOfSchema.name}", "僅查詢筆數 / 查詢資料 / 分頁查詢", HttpStatusCode.OK,
            mapOf(ContentType.Application.Json to MediaTypeObject(oneOfSchema))
        )
    }

    private val DynamicQueryResponse = buildDynamicQueryResponse(DynamicQueryResponseSchemas).createRef()

    private val DynamicQueryPagingResponse = ResponseDef(
        "DynamicQueryPagingResponse-default", "分頁查詢", HttpStatusCode.OK,
        mapOf(ContentType.Application.Json to MediaTypeObject(DynamicQueryPagingResponseSchema))
    ).createRef()


    private val DynamicQueryItemsResponse = ResponseDef(
        "DynamicQueryItemsResponse-default", "查詢資料", HttpStatusCode.OK,
        mapOf(ContentType.Application.Json to MediaTypeObject(DynamicQueryItemsResponseSchema))
    ).createRef()

    private val DynamicQueryTotalResponse = ResponseDef(
        "DynamicQueryTotalResponse", "僅查詢筆數", HttpStatusCode.OK,
        mapOf(ContentType.Application.Json to MediaTypeObject(DynamicQueryTotalResponseSchema))
    ).createRef()

    private val responseList: List<ReferenceObject> = listOf(
        EmptyBodyResponse, StringIdResponse, LongIdResponse, UUIDResponse, FreeFormDataResponse,
        DynamicQueryResponse, DynamicQueryPagingResponse, DynamicQueryItemsResponse, DynamicQueryTotalResponse
    )

    // ==================== Examples ====================

    private val exampleList: List<Example> = listOf()

    override fun loadReferenceObjects(): List<ReferenceObject> {
        return listOf(headerList, parameterList, requestBodiesList, responseList, schemaList, exampleList).flatten()
            .map { it.definition.getRef() }
    }
}

