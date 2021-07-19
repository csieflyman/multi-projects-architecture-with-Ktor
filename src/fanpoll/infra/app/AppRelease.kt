/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package fanpoll.infra.app

import fanpoll.infra.auth.*
import fanpoll.infra.auth.principal.MyPrincipal
import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.base.entity.EntityDTO
import fanpoll.infra.base.entity.EntityForm
import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.json.TaiwanInstantSerializer
import fanpoll.infra.base.response.ResponseCode
import fanpoll.infra.base.util.DateTimeUtils.TAIWAN_DATE_TIME_FORMATTER
import fanpoll.infra.database.sql.LongIdTable
import fanpoll.infra.database.sql.insert
import fanpoll.infra.database.sql.transaction
import fanpoll.infra.database.sql.update
import fanpoll.infra.database.util.ResultRowDTOMapper
import fanpoll.infra.database.util.toDTO
import io.konform.validation.Validation
import io.ktor.application.ApplicationCall
import io.ktor.response.header
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import mu.KotlinLogging
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.`java-time`.timestamp
import java.time.Instant

class AppReleaseService {

    private val logger = KotlinLogging.logger {}

    init {
        Authorization.addValidateBlock { principal, call ->
            check(principal, call)
        }
    }

    fun create(form: CreateAppReleaseForm) {
        if (form.releasedAt == null)
            form.releasedAt = Instant.now()
        else if (form.releasedAt!! < Instant.now())
            throw RequestException(ResponseCode.ENTITY_PROP_VALUE_INVALID, "appVersion ${form.appVersion} releasedAt can't be past time")

        val appOs = try {
            PrincipalSource.lookup(form.appId).type.let { AppOs.from(it) }
        } catch (e: NoSuchElementException) {
            throw RequestException(ResponseCode.ENTITY_NOT_EXIST, "appId ${form.appId} does not exist")
        }

        transaction {
            if (AppReleaseTable.select { toSQLCondition(form.appVersion) }.count() > 0)
                throw RequestException(ResponseCode.ENTITY_ALREADY_EXISTS, "appVersion ${form.appVersion} already exists")
            AppReleaseTable.insert(form) { table ->
                table[os] = appOs
            }
        }
    }

    fun update(form: UpdateAppReleaseForm) {
        if (form.releasedAt != null && form.releasedAt < Instant.now())
            throw RequestException(ResponseCode.ENTITY_PROP_VALUE_INVALID, "appVersion ${form.appVersion} releasedAt can't be past time")

        transaction {
            val dbDto = get(form.appVersion)
            if (form.releasedAt != null && dbDto.releasedAt != null && dbDto.releasedAt!! < Instant.now() && dbDto.enabled!!)
                throw RequestException(
                    ResponseCode.ENTITY_STATUS_CONFLICT,
                    "cannot update releasedAt because appVersion ${form.appVersion} had been released at " +
                            TAIWAN_DATE_TIME_FORMATTER.format(dbDto.releasedAt!!)
                )

            AppReleaseTable.update(form)
        }
    }

    fun get(appVersion: AppVersion): AppReleaseDTO {
        return AppReleaseTable.select { toSQLCondition(appVersion) }.singleOrNull()?.toDTO(AppReleaseDTO::class)
            ?: throw RequestException(ResponseCode.ENTITY_NOT_FOUND, "appVersion $appVersion does not exist")
    }

    fun check(principal: MyPrincipal, call: ApplicationCall): ClientVersionCheckResult? {
        return if (principal.source.checkClientVersion() && call.attributes.contains(ATTRIBUTE_KEY_CLIENT_VERSION)) {
            val clientVersion = call.attributes[ATTRIBUTE_KEY_CLIENT_VERSION]
            val appVersion = AppVersion(principal.source.id, clientVersion)
            logger.debug("client appVersion = $appVersion")

            val result = check(appVersion)
            call.attributes.put(ATTRIBUTE_KEY_CLIENT_VERSION_RESULT, result)
            call.response.header(HEADER_CLIENT_VERSION_CHECK_RESULT, result.name)
            result
        } else null
    }

    fun check(appVersion: AppVersion): ClientVersionCheckResult {
        val clientAppVersionNumber = appVersion.number
        val forceUpdateList = transaction {
            AppReleaseTable.slice(AppReleaseTable.forceUpdate).select {
                (AppReleaseTable.appId eq appVersion.appId) and
                        (AppReleaseTable.enabled eq true) and
                        (AppReleaseTable.releasedAt lessEq Instant.now()) and
                        (AppReleaseTable.verNum greater clientAppVersionNumber)
            }.withDistinct(true).toList().map { it[AppReleaseTable.forceUpdate] }
        }

        val result = when {
            forceUpdateList.count() == 0 -> ClientVersionCheckResult.Latest
            forceUpdateList.count() == 1 -> if (forceUpdateList.single()) ClientVersionCheckResult.ForceUpdate else ClientVersionCheckResult.Update
            else -> ClientVersionCheckResult.ForceUpdate
        }
        logger.debug { "ClientVersionCheckResult: $appVersion => $result" }
        return result
    }

    private fun SqlExpressionBuilder.toSQLCondition(appVersion: AppVersion): Op<Boolean> {
        return (AppReleaseTable.appId eq appVersion.appId) and
                (AppReleaseTable.verName eq appVersion.name)
    }
}

@Serializable
data class CreateAppReleaseForm(
    val appId: String,
    val verName: String,
    val enabled: Boolean,
    @Serializable(with = TaiwanInstantSerializer::class) var releasedAt: Instant? = null,
    val forceUpdate: Boolean,
) : EntityForm<CreateAppReleaseForm, List<String>, Long>() {

    @Transient
    val appVersion: AppVersion = AppVersion(appId, verName)

    @Transient
    val verNum: Int = AppVersion.nameToNumber(verName)

    override fun getDtoId(): List<String> = listOf(appId, verName)

    override fun validator(): Validation<CreateAppReleaseForm> = VALIDATOR

    companion object {
        private val VALIDATOR: Validation<CreateAppReleaseForm> = Validation {
            CreateAppReleaseForm::verName required { run(AppVersion.NAME_VALIDATOR) }
        }
    }
}

@Serializable
data class UpdateAppReleaseForm(
    val appId: String,
    val verName: String,
    val enabled: Boolean? = null,
    @Serializable(with = TaiwanInstantSerializer::class) val releasedAt: Instant? = null,
    val forceUpdate: Boolean? = null,
) : EntityForm<UpdateAppReleaseForm, List<String>, Long>() {

    @Transient
    val appVersion: AppVersion = AppVersion(appId, verName)

    override fun getDtoId(): List<String> = listOf(appId, verName)

    override fun validator(): Validation<UpdateAppReleaseForm> = VALIDATOR

    companion object {
        private val VALIDATOR: Validation<UpdateAppReleaseForm> = Validation {
            UpdateAppReleaseForm::verName required { run(AppVersion.NAME_VALIDATOR) }
        }
    }
}

@Serializable
data class AppReleaseDTO(val id: Long) : EntityDTO<Long> {

    var appId: String? = null
    var os: AppOs? = null
    var verName: String? = null

    var verNum: Int? = null
    var enabled: Boolean? = null

    @Serializable(with = TaiwanInstantSerializer::class)
    var releasedAt: Instant? = null
    var forceUpdate: Boolean? = null

    override fun getId(): Long = id

    companion object {
        val mapper: ResultRowDTOMapper<AppReleaseDTO> = ResultRowDTOMapper(AppReleaseDTO::class, AppReleaseTable)
    }
}

object AppReleaseTable : LongIdTable(name = "infra_app_release") {

    val appId = varchar("app_id", 30)
    val os = enumeration("os", AppOs::class)
    val verName = varchar("ver_name", 6)
    val verNum = integer("ver_num")
    val enabled = bool("enabled")
    val releasedAt = timestamp("released_at")
    val forceUpdate = bool("force_update")

    val createdAt = timestamp("created_at")
        .defaultExpression(org.jetbrains.exposed.sql.`java-time`.CurrentTimestamp())
    val updatedAt = timestamp("updated_at")
        .defaultExpression(org.jetbrains.exposed.sql.`java-time`.CurrentTimestamp())

    override val naturalKeys: List<Column<out Any>> = listOf(appId, verName)
    override val surrogateKey: Column<EntityID<Long>> = id
}