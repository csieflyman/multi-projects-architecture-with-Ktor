/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.ops.features

import fanpoll.infra.auth.authorize
import fanpoll.infra.auth.login.util.UserPasswordUtils
import fanpoll.infra.auth.principal.UserPrincipal
import fanpoll.infra.base.entity.EntityDTO
import fanpoll.infra.base.entity.EntityForm
import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.form.Form
import fanpoll.infra.base.form.ValidationUtils
import fanpoll.infra.base.i18n.Lang
import fanpoll.infra.base.json.InstantSerializer
import fanpoll.infra.base.json.UUIDSerializer
import fanpoll.infra.base.location.UUIDEntityIdLocation
import fanpoll.infra.base.response.CodeResponseDTO
import fanpoll.infra.base.response.DataResponseDTO
import fanpoll.infra.base.response.ResponseCode
import fanpoll.infra.base.response.respond
import fanpoll.infra.database.custom.lang
import fanpoll.infra.database.sql.UUIDTable
import fanpoll.infra.database.sql.insert
import fanpoll.infra.database.sql.transaction
import fanpoll.infra.database.sql.update
import fanpoll.infra.database.util.ResultRowDTOMapper
import fanpoll.infra.database.util.queryDB
import fanpoll.infra.database.util.toDTO
import fanpoll.infra.notification.senders.NotificationSender
import fanpoll.infra.notification.util.SendNotificationForm
import fanpoll.infra.openapi.dynamicQuery
import fanpoll.infra.openapi.post
import fanpoll.infra.openapi.put
import fanpoll.infra.openapi.schema.operation.support.OpenApiModel
import fanpoll.ops.*
import io.konform.validation.Validation
import io.konform.validation.jsonschema.maxLength
import io.ktor.application.call
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.route
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.`java-time`.timestamp
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.koin.ktor.ext.inject
import java.time.Instant
import java.util.*

fun Routing.opsUser() {

    val opsUserService by inject<OpsUserService>()
    val notificationSender by inject<NotificationSender>()

    route("${OpsConst.urlRootPath}/users") {

        authorize(OpsAuth.Root) {

            post<CreateUserForm, UUID>(OpsOpenApi.CreateUser) { form ->
                val id = opsUserService.createUser(form)
                call.respond(DataResponseDTO.uuid(id))
            }

            put<UUIDEntityIdLocation, UpdateUserForm, Unit>(OpsOpenApi.UpdateUser) { _, form ->
                opsUserService.updateUser(form)
                call.respond(HttpStatusCode.OK)
            }

            dynamicQuery<UserDTO>(OpsOpenApi.FindUsers) { dynamicQuery ->
                call.respond(dynamicQuery.queryDB<UserDTO>())
            }

            post<SendNotificationForm, UUID>("/sendNotification", OpsOpenApi.SendNotification) { form ->
                val notification = form.toNotification(OpsNotification.SendNotification)
                notificationSender.send(notification)
                call.respond(DataResponseDTO.uuid(notification.id))
            }
        }

        authorize(OpsAuth.User) {

            put<UpdateUserPasswordForm, Unit>("/myPassword", OpsOpenApi.UpdateMyPassword) { form ->
                val userId = call.principal<UserPrincipal>()!!.userId
                opsUserService.updatePassword(userId, form)
                call.respond(CodeResponseDTO.OK)
            }
        }
    }
}

class OpsUserService {

    fun createUser(form: CreateUserForm): UUID {
        form.password = UserPasswordUtils.hashPassword(form.password)
        return transaction {
            if (OpsUserTable.select { OpsUserTable.account eq form.account }.count() > 0)
                throw RequestException(ResponseCode.ENTITY_ALREADY_EXISTS, "${form.account} already exists")
            OpsUserTable.insert(form) as UUID
        }
    }

    fun updateUser(form: UpdateUserForm) {
        transaction {
            OpsUserTable.update(form)
        }
    }

    fun getUserById(userId: UUID): UserDTO {
        return transaction {
            OpsUserTable.select { OpsUserTable.id eq userId }.single().toDTO(UserDTO::class)
        }
    }

    fun updatePassword(userId: UUID, form: UpdateUserPasswordForm) {
        transaction {
            val hashedPassword = OpsUserTable.slice(OpsUserTable.password).select { OpsUserTable.id eq userId }
                .singleOrNull()?.let { it[OpsUserTable.password] }
                ?: throw RequestException(ResponseCode.ENTITY_NOT_EXIST, "user $userId does not exist")
            if (UserPasswordUtils.verifyPassword(form.oldPassword, hashedPassword)) {
                OpsUserTable.update({ OpsUserTable.id eq userId }) {
                    it[password] = UserPasswordUtils.hashPassword(form.newPassword)
                }
            } else throw RequestException(ResponseCode.AUTH_BAD_PASSWORD)
        }
    }
}

@OpenApiModel(propertyNameOrder = ["account", "password", "enabled", "role", "name"])
@Serializable
data class CreateUserForm(
    val account: String,
    var password: String,
    val enabled: Boolean = true,
    val role: OpsUserRole,
    val name: String,
    val email: String,
    val mobile: String? = null,
    val lang: Lang? = null,
) : EntityForm<CreateUserForm, String, UUID>() {

    @Transient
    val id: UUID = UUID.randomUUID()

    override fun getEntityId(): UUID = id

    override fun getDtoId(): String = account

    override fun validator(): Validation<CreateUserForm> = VALIDATOR

    companion object {
        private val VALIDATOR: Validation<CreateUserForm> = Validation {
            CreateUserForm::account required { run(ValidationUtils.EMAIL_VALIDATOR) }
            CreateUserForm::password required { run(ValidationUtils.PASSWORD_VALIDATOR) }
            CreateUserForm::name required { maxLength(USER_NAME_LENGTH) }
            CreateUserForm::email required { run(ValidationUtils.EMAIL_VALIDATOR) }
            CreateUserForm::mobile ifPresent { run(ValidationUtils.TAIWAN_MOBILE_NUMBER_VALIDATOR) }
        }
    }
}

@OpenApiModel(propertyNameOrder = ["id", "enabled", "role", "name"])
@Serializable
data class UpdateUserForm(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    val enabled: Boolean? = null,
    val role: OpsUserRole? = null,
    val name: String? = null,
    val email: String? = null,
    val mobile: String? = null,
    val lang: Lang? = null
) : EntityForm<UpdateUserForm, String, UUID>() {

    override fun getEntityId(): UUID = id

    override fun validator(): Validation<UpdateUserForm> = VALIDATOR

    companion object {
        private val VALIDATOR: Validation<UpdateUserForm> = Validation {
            UpdateUserForm::name ifPresent { maxLength(USER_NAME_LENGTH) }
            UpdateUserForm::email ifPresent { run(ValidationUtils.EMAIL_VALIDATOR) }
            UpdateUserForm::mobile ifPresent { run(ValidationUtils.TAIWAN_MOBILE_NUMBER_VALIDATOR) }
        }
    }
}

@OpenApiModel(propertyNameOrder = ["id", "account", "enabled", "role", "name"])
@Serializable
data class UserDTO(@JvmField @Serializable(with = UUIDSerializer::class) val id: UUID) : EntityDTO<UUID> {

    var account: String? = null
    var enabled: Boolean? = null
    var role: OpsUserRole? = null

    var name: String? = null
    var email: String? = null
    var mobile: String? = null
    var lang: Lang? = null

    @Transient
    var password: String? = null

    @Serializable(with = InstantSerializer::class)
    var createdAt: Instant? = null

    override fun getId(): UUID = id

    companion object {
        val mapper: ResultRowDTOMapper<UserDTO> = ResultRowDTOMapper(UserDTO::class, OpsUserTable)
    }
}

@Serializable
data class UpdateUserPasswordForm(val oldPassword: String, val newPassword: String) : Form<UpdateUserPasswordForm>() {

    override fun validator(): Validation<UpdateUserPasswordForm> = VALIDATOR

    companion object {
        private val VALIDATOR: Validation<UpdateUserPasswordForm> = Validation {
            UpdateUserPasswordForm::oldPassword required { run(ValidationUtils.PASSWORD_VALIDATOR) }
            UpdateUserPasswordForm::newPassword required { run(ValidationUtils.PASSWORD_VALIDATOR) }
        }
    }
}

object OpsUserTable : UUIDTable(name = "ops_user") {

    val account = varchar("account", ValidationUtils.EMAIL_MAX_LENGTH) //unique
    val enabled = bool("enabled")
    val role = enumerationByName("role", 20, OpsUserRole::class)

    val name = varchar("name", USER_NAME_LENGTH)
    val email = varchar("email", ValidationUtils.EMAIL_MAX_LENGTH)
    val mobile = varchar("mobile", ValidationUtils.TAIWAN_MOBILE_NUMBER_LENGTH).nullable()
    val lang = lang("lang").nullable()

    val password = varchar("password", 1000)
    val createdAt = timestamp("created_at")
        .defaultExpression(org.jetbrains.exposed.sql.`java-time`.CurrentTimestamp())
    val updatedAt = timestamp("updated_at")
        .defaultExpression(org.jetbrains.exposed.sql.`java-time`.CurrentTimestamp())

    override val naturalKeys: List<Column<out Any>> = listOf(account)
    override val surrogateKey: Column<EntityID<UUID>> = id
}

private const val USER_NAME_LENGTH = 30