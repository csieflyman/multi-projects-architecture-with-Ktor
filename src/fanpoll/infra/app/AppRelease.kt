/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package fanpoll.infra.app

import fanpoll.infra.RequestBodyException
import fanpoll.infra.auth.*
import fanpoll.infra.controller.EntityDTO
import fanpoll.infra.controller.EntityForm
import fanpoll.infra.database.*
import fanpoll.infra.utils.DateTimeUtils.TAIWAN_DATE_TIME_FORMATTER
import fanpoll.infra.utils.TaiwanInstantSerializer
import io.konform.validation.Validation
import io.ktor.application.ApplicationCall
import io.ktor.response.header
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import mu.KotlinLogging
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.`java-time`.timestamp
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import java.time.Instant

object AppReleaseService {

    private val logger = KotlinLogging.logger {}

    fun init() {
        Authorization.addValidateBlock { principal, call ->
            check(principal, call)
        }
    }

    fun create(form: CreateAppReleaseForm) {
        if (form.releaseTime == null)
            form.releaseTime = Instant.now()
        else if (form.releaseTime!! < Instant.now())
            throw RequestBodyException("appVersion ${form.appVersion} releaseTime can't be past time")

        val appOs = try {
            PrincipalSource.lookup(form.appId).userDeviceType?.let { AppOs.from(it) }
        } catch (e: NoSuchElementException) {
            throw RequestBodyException("appId ${form.appId} is not exist")
        } ?: throw RequestBodyException("appId ${form.appId} is not app service")

        myTransaction {
            if (AppReleaseTable.select { toSQLCondition(form.appVersion) }.count() > 0)
                throw RequestBodyException("appVersion ${form.appVersion} is already exist")
            AppReleaseTable.insert(form) { table ->
                table[os] = appOs
            }
        }
    }

    fun update(form: UpdateAppReleaseForm) {
        if (form.releaseTime != null && form.releaseTime < Instant.now())
            throw RequestBodyException("appVersion ${form.appVersion} releaseTime can't be past time")

        myTransaction {
            val dbDto = AppReleaseTable.select { toSQLCondition(form.appVersion) }
                .singleOrNull()?.toDTO(AppReleaseDTO::class)
                ?: throw RequestBodyException("appVersion ${form.appVersion} is not exist")
            if (form.releaseTime != null && dbDto.releaseTime != null && dbDto.releaseTime!! < Instant.now() && dbDto.enabled!!)
                throw RequestBodyException(
                    "cannot update releaseTime because appVersion ${form.appVersion} had been released at " +
                            TAIWAN_DATE_TIME_FORMATTER.format(dbDto.releaseTime!!)
                )

            AppReleaseTable.update(form)
        }
    }

    fun get(appVersion: AppVersion): AppReleaseDTO {
        return AppReleaseTable.select { toSQLCondition(appVersion) }.singleOrNull()
            ?.toDTO(AppReleaseDTO::class)
            ?: throw RequestBodyException("appVersion $appVersion is not exist")
    }

    fun check(principal: MyPrincipal, call: ApplicationCall): ClientVersionCheckResult? {
        return if (principal.source.checkClientVersion && call.attributes.contains(ATTRIBUTE_KEY_CLIENT_VERSION)) {
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
        val forceUpdateList = myTransaction {
            AppReleaseTable.slice(AppReleaseTable.forceUpdate).select {
                (AppReleaseTable.appId eq appVersion.appId) and
                        (AppReleaseTable.enabled eq true) and
                        (AppReleaseTable.releaseTime lessEq Instant.now()) and
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

    private fun toSQLCondition(appVersion: AppVersion): Op<Boolean> {
        return (AppReleaseTable.appId eq appVersion.appId) and
                (AppReleaseTable.verName eq appVersion.name)
    }
}

@Serializable
data class CreateAppReleaseForm(
    val appId: String,
    val verName: String,
    val enabled: Boolean,
    @Serializable(with = TaiwanInstantSerializer::class) var releaseTime: Instant? = null,
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
    @Serializable(with = TaiwanInstantSerializer::class) val releaseTime: Instant? = null,
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
    var releaseTime: Instant? = null
    var forceUpdate: Boolean? = null

    override fun getId(): Long = id

    companion object {
        val mapper: ResultRowDTOMapper<AppReleaseDTO> = ResultRowDTOMapper(AppReleaseDTO::class, AppReleaseTable)
    }
}

object AppReleaseTable : LongIdDTOTable(name = "infra_app_release") {

    val appId = varchar("app_id", 30)
    val os = enumeration("os", AppOs::class)
    val verName = varchar("ver_name", 6)
    val verNum = integer("ver_num")
    val enabled = bool("enabled")
    val releaseTime = timestamp("release_time")
    val forceUpdate = bool("force_update")

    val createTime = timestamp("create_time")
        .defaultExpression(org.jetbrains.exposed.sql.`java-time`.CurrentTimestamp())
    val updateTime = timestamp("update_time")
        .defaultExpression(org.jetbrains.exposed.sql.`java-time`.CurrentTimestamp())

    override val naturalKeys: List<Column<out Any>> = listOf(appId, verName)
    override val surrogateKey: Column<EntityID<Long>> = id
}