/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.club.features

import fanpoll.club.ClubUserRole
import fanpoll.club.ClubUserType
import fanpoll.infra.InternalServerErrorException
import fanpoll.infra.RequestBodyException
import fanpoll.infra.RequestException
import fanpoll.infra.ResponseCode
import fanpoll.infra.app.UserDeviceDTO
import fanpoll.infra.app.UserDeviceTable
import fanpoll.infra.auth.UserPrincipal
import fanpoll.infra.controller.EntityDTO
import fanpoll.infra.controller.EntityForm
import fanpoll.infra.controller.Form
import fanpoll.infra.controller.ValidationUtils
import fanpoll.infra.database.*
import fanpoll.infra.login.AppLoginForm
import fanpoll.infra.login.LoginResultCode
import fanpoll.infra.login.LoginService
import fanpoll.infra.login.UserPasswordUtils
import fanpoll.infra.openapi.schema.operation.support.OpenApiModel
import fanpoll.infra.utils.InstantSerializer
import fanpoll.infra.utils.UUIDSerializer
import io.konform.validation.Validation
import io.konform.validation.jsonschema.maxLength
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import mu.KotlinLogging
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.`java-time`.timestamp
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.*

object UserService {

    private val logger = KotlinLogging.logger {}

    fun createUser(form: CreateUserForm): UUID {
        form.password = UserPasswordUtils.hashPassword(form.password)
        return myTransaction {
            if (ClubUserTable.select { ClubUserTable.account eq form.account }.count() > 0)
                throw RequestBodyException("${form.account} is already exist")
            ClubUserTable.insert(form) as UUID
        }
    }

    fun updateUser(form: UpdateUserForm) {
        myTransaction {
            ClubUserTable.update(form)
        }
    }

    fun updatePassword(userId: UUID, form: UpdateUserPasswordForm) {
        myTransaction {
            val hashedPassword = ClubUserTable.slice(ClubUserTable.password).select { ClubUserTable.id eq userId }
                .singleOrNull()?.let { it[ClubUserTable.password] }
                ?: throw RequestBodyException("user $userId is not exist")
            if (UserPasswordUtils.verifyPassword(form.oldPassword, hashedPassword)) {
                ClubUserTable.update({ ClubUserTable.id eq userId }) {
                    it[password] = UserPasswordUtils.hashPassword(form.newPassword)
                }
            } else throw RequestException(ResponseCode.AUTH_BAD_PASSWORD)
        }
    }

    fun login(form: AppLoginForm): String {
        val user = myTransaction {
            ClubUserTable.join(
                UserDeviceTable, JoinType.LEFT,
                PgSQLUUIDExpression(ClubUserTable.id), PgSQLUUIDExpression(UserDeviceTable.userId)
            ) {
                (UserDeviceTable.type eq form.appOs.toUserDeviceType())
            }.slice(
                ClubUserTable.id, ClubUserTable.account, ClubUserTable.enabled, ClubUserTable.role, ClubUserTable.password,
                UserDeviceTable.id, UserDeviceTable.osVersion, UserDeviceTable.pushToken
            ).select { ClubUserTable.account eq form.account }.toList().toSingleDTO(UserDTO::class)
                ?: throw RequestException(ResponseCode.AUTH_LOGIN_UNAUTHENTICATED)
        }

        val loginResultCode: LoginResultCode = if (!user.enabled!!) {
            LoginResultCode.ACCOUNT_DISABLED
        } else if (!UserPasswordUtils.verifyPassword(form.password, user.password!!)) {
            LoginResultCode.BAD_CREDENTIAL
        } else LoginResultCode.SUCCESS

        form.populateUser(ClubUserType.User.value, user.id, setOf(user.role!!.value))
        val loginResult = LoginService.appLogin(form, loginResultCode)

        return if (loginResultCode == LoginResultCode.SUCCESS) {
            if (form.deviceId != null)
                createOrUpdateUserDevice(user, form)
            loginResult.userPrincipal!!.sessionId()
        } else // LoginService.login throw exception if LoginResultCode != SUCCESS
            throw InternalServerErrorException(ResponseCode.UNEXPECTED_ERROR)
    }

    private fun createOrUpdateUserDevice(user: UserDTO, formDTO: AppLoginForm) {
        val device = user.devices?.find { it.id == formDTO.deviceId }
        if (device == null) {
            myTransaction {
                UserDeviceTable.insert(formDTO.toCreateUserDeviceDTO(ClubUserType.User.value, user.id))
            }
        } else {
            val updateDTO = formDTO.toUpdateUserDeviceDTO()
            if (device.osVersion != updateDTO.osVersion || device.pushToken != updateDTO.pushToken) {
                myTransaction {
                    UserDeviceTable.update(updateDTO)
                }
            }
        }
    }

    fun logout(principal: UserPrincipal) {
        LoginService.logout(principal)
    }
}

@OpenApiModel(propertyNameOrder = ["account", "password", "name", "enabled", "role"])
@Serializable
data class CreateUserForm(
    val account: String,
    val name: String,
    val age: Int?,
    val enabled: Boolean = true,
    val role: ClubUserRole,
    var password: String
) : EntityForm<CreateUserForm, String, UUID>() {

    @Serializable(with = UUIDSerializer::class)
    val id: UUID = UUID.randomUUID()

    override fun getEntityId(): UUID = id

    override fun getDtoId(): String = account

    override fun validator(): Validation<CreateUserForm> = VALIDATOR

    companion object {
        private val VALIDATOR: Validation<CreateUserForm> = Validation {
            CreateUserForm::account required { run(ValidationUtils.EMAIL_VALIDATOR) }
            CreateUserForm::name required { maxLength(30) }
            CreateUserForm::password required { run(ValidationUtils.PASSWORD_VALIDATOR) }
        }
    }
}

@OpenApiModel(propertyNameOrder = ["id", "name", "enabled", "role"])
@Serializable
data class UpdateUserForm(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    val name: String? = null,
    val age: Int? = null,
    val enabled: Boolean? = null,
    val role: ClubUserRole? = null
) : EntityForm<UpdateUserForm, String, UUID>() {

    override fun getEntityId(): UUID = id

    override fun validator(): Validation<UpdateUserForm> = VALIDATOR

    companion object {
        private val VALIDATOR: Validation<UpdateUserForm> = Validation {
            UpdateUserForm::name required { maxLength(30) }
        }
    }
}

@OpenApiModel(propertyNameOrder = ["id", "account", "name", "enabled", "role"])
@Serializable
data class UserDTO(@JvmField @Serializable(with = UUIDSerializer::class) val id: UUID) : EntityDTO<UUID> {

    var account: String? = null
    var name: String? = null
    var age: Int? = null
    var enabled: Boolean? = null
    var role: ClubUserRole? = null

    @Transient
    var password: String? = null

    @Serializable(with = InstantSerializer::class)
    var createTime: Instant? = null

    var devices: List<UserDeviceDTO>? = null

    override fun getId(): UUID = id

    companion object {
        val mapper: ResultRowDTOMapper<UserDTO> = ResultRowDTOMapper(
            UserDTO::class, ClubUserTable,
            joins = listOf(DynamicDBJoinPart(JoinType.LEFT, UserDeviceTable, ClubUserTable.id, UserDeviceTable.userId))
        )
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

object ClubUserTable : UUIDDTOTable(name = "club_user") {

    val account = varchar("account", ValidationUtils.EMAIL_MAX_LENGTH) //unique
    val name = varchar("name", 30)
    val age = integer("age")
    val enabled = bool("enabled")
    val role = enumeration("role", ClubUserRole::class)
    val password = varchar("password", 1000)
    val createTime = timestamp("create_time")
        .defaultExpression(org.jetbrains.exposed.sql.`java-time`.CurrentTimestamp())
    val updateTime = timestamp("update_time")
        .defaultExpression(org.jetbrains.exposed.sql.`java-time`.CurrentTimestamp())

    override val naturalKeys: List<Column<out Any>> = listOf(account)
    override val surrogateKey: Column<EntityID<UUID>> = id
}