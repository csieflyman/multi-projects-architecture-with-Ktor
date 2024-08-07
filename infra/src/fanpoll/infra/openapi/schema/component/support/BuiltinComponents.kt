/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.schema.component.support

import fanpoll.infra.auth.AuthConst
import fanpoll.infra.auth.login.ClientVersionCheckResult
import fanpoll.infra.auth.principal.ClientAttributeKey
import fanpoll.infra.auth.provider.UserRunAsAuthProvider
import fanpoll.infra.base.response.*
import fanpoll.infra.openapi.schema.component.definitions.ComponentsObject
import fanpoll.infra.openapi.schema.operation.definitions.*
import fanpoll.infra.openapi.schema.operation.support.Example
import fanpoll.infra.openapi.schema.operation.support.Response
import fanpoll.infra.openapi.schema.operation.support.Schema
import fanpoll.infra.openapi.schema.operation.support.converters.ResponseObjectConverter
import fanpoll.infra.openapi.schema.operation.support.converters.SchemaObjectConverter
import fanpoll.infra.openapi.schema.operation.support.utils.ResponseUtils
import fanpoll.infra.release.app.domain.AppVersion
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlin.reflect.KType

object BuiltinComponents {

    // ==================== Schemas ====================

    val ResponseCodeSchema = buildResponseCodeSchema().createRef()

    private fun buildResponseCodeSchema(): ModelDef {
        val requiredProperties = listOf("value", "name", "type")
        val properties: Map<String, Schema> = listOf(
            PropertyDef("value", SchemaDataType.string),
            PropertyDef("name", SchemaDataType.string),
            PropertyDef(
                "type", SchemaDataType.string,
                refName = "codeType", enum = ResponseCodeType.entries.map { it.name }.toList(), kClass = ResponseCodeType::class
            ),
        ).associate { it.valuePair() }

        return ModelDef(
            ResponseCode::class.simpleName!!, requiredProperties, properties,
            kClass = ResponseCode::class, refName = "code"
        ).also {
            it.properties.values.forEach { property -> (property.getDefinition() as SchemaObject).parent = it }
        }
    }

    private val InfraResponseCodeValueSchema = PropertyDef(
        "InfraResponseCode", SchemaDataType.string,
        ResponseUtils.buildResponseCodesDescription(InfraResponseCode.AllCodes),
        enum = InfraResponseCode.AllCodes.map { it.value }.toList()
    )

    private val ErrorResponseErrorsSchema = buildErrorResponseErrorsSchema().createRef()

    private fun buildErrorResponseErrorsSchema(): ArrayModelDef {
        val requiredProperties = listOf("code", "detail")
        val properties: Map<String, Schema> = listOf(
            ResponseCodeSchema,
            PropertyDef("detail", SchemaDataType.string, "detail message for developer"),
            DictionaryPropertyDef("data", description = "JsonObject or JsonArray")
        ).associate { it.valuePair() } as Map<String, Schema>

        val modelDef = ModelDef(
            ErrorResponseDetailError::class.simpleName!!, requiredProperties, properties,
            kClass = ErrorResponseDetailError::class
        ).also {
            it.properties.values.forEach { property -> (property.getDefinition() as SchemaObject).parent = it }
        }
        return ArrayModelDef(
            "${ErrorResponseDetailError::class.simpleName!!}Array", modelDef,
            "all detail errors", refName = "errors"
        )
    }

    val ErrorResponseSchema = buildErrorResponseSchema()

    private fun buildErrorResponseSchema(): ModelDef {
        val requiredProperties = listOf("code", "message", "detail", "reqId")
        val properties: Map<String, Schema> = listOf(
            ResponseCodeSchema,
            PropertyDef("message", SchemaDataType.string, "message for user"),
            PropertyDef("detail", SchemaDataType.string, "detail message for developer"),
            PropertyDef("reqId", SchemaDataType.string, "request unique id"),
            DictionaryPropertyDef("data", description = "JsonObject or JsonArray"),
            ErrorResponseErrorsSchema
        ).associate { it.valuePair() } as Map<String, Schema>

        return ModelDef(ErrorResponseDTO::class.simpleName!!, requiredProperties, properties, kClass = ErrorResponseDTO::class).also {
            it.properties.values.forEach { property -> (property.getDefinition() as SchemaObject).parent = it }
        }
    }

    private val DynamicQueryPagingResponseSchema =
        buildDynamicQueryPagingResponseSchema(DictionaryPropertyDef("default", description = "JsonObject of JsonArray"))

    private fun buildDynamicQueryPagingResponseSchema(itemSchema: Schema): ModelDef {
        val requiredProperties = listOf("code", "data")
        val properties: Map<String, Schema> = listOf(
            ResponseCodeSchema,
            ModelDef(
                "data", listOf("total", "totalPages", "itemsPerPage", "pageIndex", "items"),
                listOf(
                    PropertyDef("total", SchemaDataType.integer),
                    PropertyDef("totalPages", SchemaDataType.integer),
                    PropertyDef("itemsPerPage", SchemaDataType.integer),
                    PropertyDef("pageIndex", SchemaDataType.integer),
                    ArrayModelDef("items", itemSchema),
                ).associate { it.valuePair() }
            ).also {
                it.properties.values.forEach { property -> (property.getDefinition() as SchemaObject).parent = it }
            }
        ).associate { it.valuePair() } as Map<String, Schema>

        return ModelDef("DynamicQueryPagingResponse-${itemSchema.name}", requiredProperties, properties).also {
            it.properties.values.forEach { property -> (property.getDefinition() as SchemaObject).parent = it }
        }
    }

    private val DynamicQueryItemsResponseSchema =
        buildDynamicQueryItemsResponseSchema(DictionaryPropertyDef("default", description = "JsonObject of JsonArray"))

    private fun buildDynamicQueryItemsResponseSchema(itemSchema: Schema): ModelDef {
        val requiredProperties = listOf("code", "data")
        val properties: Map<String, Schema> = listOf(
            ResponseCodeSchema,
            ArrayModelDef("data", itemSchema),
        ).associate { it.valuePair() } as Map<String, Schema>

        return ModelDef("DynamicQueryItemsResponse-${itemSchema.name}", requiredProperties, properties).also {
            it.properties.values.forEach { property -> (property.getDefinition() as SchemaObject).parent = it }
        }
    }

    private val DynamicQueryTotalResponseSchema = buildDynamicQueryTotalResponseSchema()

    private fun buildDynamicQueryTotalResponseSchema(): ModelDef {
        val requiredProperties = listOf("code", "data")
        val properties: Map<String, Schema> = listOf(
            ResponseCodeSchema,
            ModelDef(
                "data", listOf("total"),
                mapOf(PropertyDef("total", SchemaDataType.integer).valuePair())
            ),
        ).associate { it.valuePair() } as Map<String, Schema>

        return ModelDef("DynamicQueryTotalResponse", requiredProperties, properties).also {
            it.properties.values.forEach { property -> (property.getDefinition() as SchemaObject).parent = it }
        }
    }

    private val DynamicQueryResponseSchemas = OneOfSchema(
        "default",
        listOf(DynamicQueryPagingResponseSchema, DynamicQueryItemsResponseSchema, DynamicQueryTotalResponseSchema).map { it.createRef() }
    )

    // ==================== Parameter Schema ====================

    private val sessionIdSchema = PropertyDef(
        AuthConst.SESSION_ID_HEADER_NAME, SchemaDataType.string
    )

    private val runAsTokenSchema = PropertyDef(
        UserRunAsAuthProvider.RUN_AS_TOKEN_HEADER_NAME, SchemaDataType.string,
        description = UserRunAsAuthProvider.tokenPatternDescription
    )

    private val schemaList: List<ReferenceObject> = listOf(
        ResponseCodeSchema, InfraResponseCodeValueSchema,
        ErrorResponseSchema, ErrorResponseErrorsSchema,
        DynamicQueryPagingResponseSchema, DynamicQueryItemsResponseSchema, DynamicQueryTotalResponseSchema
    ).map { it.createRef() }

    // ==================== Headers ====================

    private val ClientVersionCheckResultSchema = PropertyDef(
        ClientAttributeKey.CHECK_RESULT.name, SchemaDataType.string,
        enum = ClientVersionCheckResult.entries, kClass = ClientVersionCheckResult::class,
        example = ClientVersionCheckResult.Latest
    )

    private val ClientVersionCheckResultResponseHeader = HeaderObject(
        true, ClientVersionCheckResultSchema
    ).createRef()

    private val headerList: List<ReferenceObject> = listOf(
        ClientVersionCheckResultResponseHeader
    )

    // ==================== Parameters ====================

    // ===== Parameters(Header) ======

    val SessionIdHeader = ParameterObject(ParameterInputType.header, true, sessionIdSchema).createRef()
    val SessionIdOptionalHeader = ParameterObject(ParameterInputType.header, false, sessionIdSchema).createRef()

    private val RunAsOptionalHeader = ParameterObject(ParameterInputType.header, false, runAsTokenSchema).createRef()

    private val ClientVersionSchema = PropertyDef(
        ClientAttributeKey.CLIENT_VERSION.name, SchemaDataType.string,
        pattern = AppVersion.NAME_PATTERN,
        description = "App 端須帶入程式版本號，Server 會回傳驗證結果至 response header => ${ClientAttributeKey.CLIENT_ID.name} = ${
            ClientVersionCheckResult.entries
        }",
        example = "1.0.0"
    )

    val ClientVersionOptionalHeader = ParameterObject(ParameterInputType.header, false, ClientVersionSchema).createRef()

    private val headerParameterList: List<ReferenceObject> = listOf(
        SessionIdHeader, SessionIdOptionalHeader, RunAsOptionalHeader, ClientVersionOptionalHeader
    )

    // ===== Parameters(Query) ======

    val DynamicQueryParameters: List<ReferenceObject> = listOf(
        ParameterObject(
            ParameterInputType.query, false,
            PropertyDef("q_fields", SchemaDataType.string),
            "回傳欄位 => 可指定多個欄位，以逗號分隔。如果不指定則回傳所有欄位。"
        ).createRef(),
        ParameterObject(
            ParameterInputType.query, false, PropertyDef("q_filter", SchemaDataType.string),
            "查詢條件 DSL => 以 '[' 字元開始， ']' 字元結束，" +
                    "每個運算式 expression 的格式是 field operator value ，中間以空白字元分隔，多個條件以 and 或 or 連接 (目前不支援巢狀條件)。" +
                    "operator 支援 = != > >= < <= like in not_in is_null is_not_null 。" +
                    "範例: [name = james and age >= 18 and enabled = true and role in (admin, member) and createdAt >= 2021-01-01T00:00:00Z]"
        ).createRef(),
        ParameterObject(
            ParameterInputType.query, false, PropertyDef("q_orderBy", SchemaDataType.string),
            "指定排序欄位 => 可指定多個欄位，以逗號分隔, 例如 name,price- 代表先以 name 欄位 asc 排序，再以 price 欄位 desc 排序 (+ 號代表 asc 預設可省略, - 號代表 desc)",
        ).createRef(),
        ParameterObject(
            ParameterInputType.query, false, PropertyDef("q_offset", SchemaDataType.integer),
            "限制回傳筆數 => 需一併指定 q_limit，從第 q_offset 筆(start from 0)，回傳 q_limit 筆資料"
        ).createRef(),
        ParameterObject(
            ParameterInputType.query, false, PropertyDef("q_limit", SchemaDataType.integer),
            "限制回傳筆數 => 需一併指定 q_offset，從第 q_offset 筆(start from 0)，回傳 q_limit 筆資料"
        ).createRef(),
        ParameterObject(
            ParameterInputType.query, false, PropertyDef("q_pageIndex", SchemaDataType.integer),
            "分頁查詢 => 需一併指定 q_itemsPerPage，回傳第 q_pageIndex 頁，每頁 q_itemsPerPage 筆"
        ).createRef(),
        ParameterObject(
            ParameterInputType.query, false, PropertyDef("q_itemsPerPage", SchemaDataType.integer),
            "分頁查詢 => 需一併指定 q_pageIndex，回傳第 q_pageIndex 頁，每頁 q_itemsPerPage 筆"
        ).createRef(),
        ParameterObject(
            ParameterInputType.query, false, PropertyDef("q_count", SchemaDataType.boolean),
            "僅回傳資料筆數 (預設值 false)"
        ).createRef()
    )

    private val parameterList: List<ReferenceObject> = listOf(headerParameterList, DynamicQueryParameters).flatten()

    // ==================== RequestBodies ====================

    private val requestBodiesList: List<ReferenceObject> = listOf()

    // ==================== Responses ====================

    val EmptyBodyResponse = ResponseObject(
        "EmptyBodyResponse", "Success", HttpStatusCode.OK
    ).createRef()

    val StringIdResponse = ResponseObjectConverter.toDataResponse(
        ModelDef(
            "StringId", listOf("id"),
            mapOf("id" to PropertyDef("id", SchemaDataType.string)),
            "object created and return object string id"
        )
    ).createRef()

    val UUIDResponse = ResponseObjectConverter.toDataResponse(
        ModelDef(
            "UUID", listOf("id"),
            mapOf("id" to PropertyDef("id", SchemaDataType.string, format = "uuid")),
            "object created and return object uuid"
        )
    ).createRef()

    val LongIdResponse = ResponseObjectConverter.toDataResponse(
        ModelDef(
            "LongId", listOf("id"),
            mapOf("id" to PropertyDef("id", SchemaDataType.integer)),
            "object created and return object integer id"
        )
    ).createRef()

    val FreeFormDataResponse = ResponseObjectConverter.toDataResponse(
        DictionaryPropertyDef("FreeForm", description = "JsonObject or JsonArray")
    ).createRef()

    val DefaultErrorResponse = ResponseObject(
        "DefaultErrorResponse", "Check out ResponseCode and ErrorResponse Schema", null,
        mapOf(ContentType.Application.Json to MediaTypeObject(ErrorResponseSchema))
    ).createRef()

    fun buildDynamicQueryResponse(components: ComponentsObject, responseBodyType: KType): Response {
        val dtoSchema = SchemaObjectConverter.toSchema(components, responseBodyType)
        val dtoSchemaRef = components.add(dtoSchema.getDefinition())
        val dynamicQuerySchemas = buildDynamicQueryResponseSchemas(components, dtoSchemaRef)
        return buildDynamicQueryResponse(dynamicQuerySchemas)
    }

    private fun buildDynamicQueryResponseSchemas(components: ComponentsObject, itemSchema: Schema): OneOfSchema {
        val pagingResponseSchema = buildDynamicQueryPagingResponseSchema(itemSchema)
        val itemsResponseSchema = buildDynamicQueryItemsResponseSchema(itemSchema)
        components.add(pagingResponseSchema)
        components.add(itemsResponseSchema)
        return OneOfSchema(itemSchema.name, listOf(
            pagingResponseSchema,
            itemsResponseSchema,
            DynamicQueryTotalResponseSchema
        ).map { it.createRef() })
    }

    private fun buildDynamicQueryResponse(oneOfSchema: OneOfSchema): ResponseObject {
        return ResponseObject(
            "DynamicQueryPagingResponse-${oneOfSchema.name}", "僅查詢筆數 / 查詢資料 / 分頁查詢", HttpStatusCode.OK,
            mapOf(ContentType.Application.Json to MediaTypeObject(oneOfSchema))
        )
    }

    private val DynamicQueryResponse = buildDynamicQueryResponse(DynamicQueryResponseSchemas).createRef()

    private val DynamicQueryPagingResponse = ResponseObject(
        "DynamicQueryPagingResponse-default", "分頁查詢", HttpStatusCode.OK,
        mapOf(ContentType.Application.Json to MediaTypeObject(DynamicQueryPagingResponseSchema))
    ).createRef()


    private val DynamicQueryItemsResponse = ResponseObject(
        "DynamicQueryItemsResponse-default", "查詢資料", HttpStatusCode.OK,
        mapOf(ContentType.Application.Json to MediaTypeObject(DynamicQueryItemsResponseSchema))
    ).createRef()

    private val DynamicQueryTotalResponse = ResponseObject(
        "DynamicQueryTotalResponse", "僅查詢筆數", HttpStatusCode.OK,
        mapOf(ContentType.Application.Json to MediaTypeObject(DynamicQueryTotalResponseSchema))
    ).createRef()

    private val responseList: List<ReferenceObject> = listOf(
        EmptyBodyResponse, StringIdResponse, LongIdResponse, UUIDResponse, FreeFormDataResponse,
        DefaultErrorResponse,
        DynamicQueryResponse, DynamicQueryPagingResponse, DynamicQueryItemsResponse, DynamicQueryTotalResponse
    )

    // ==================== Examples ====================

    private val exampleList: List<Example> = listOf()

    val components = listOf(headerList, parameterList, requestBodiesList, responseList, schemaList, exampleList).flatten()
        .map { it.getReference() }
}