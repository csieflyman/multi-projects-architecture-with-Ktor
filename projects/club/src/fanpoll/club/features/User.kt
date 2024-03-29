/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.club.features

import fanpoll.club.ClubAuth
import fanpoll.club.ClubConst
import fanpoll.club.ClubOpenApi
import fanpoll.club.ClubUserRole
import fanpoll.infra.app.UserDeviceDTO
import fanpoll.infra.app.UserDeviceTable
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
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.base.response.respond
import fanpoll.infra.database.custom.lang
import fanpoll.infra.database.sql.UUIDTable
import fanpoll.infra.database.sql.insert
import fanpoll.infra.database.sql.transaction
import fanpoll.infra.database.sql.update
import fanpoll.infra.database.util.DynamicDBJoinPart
import fanpoll.infra.database.util.ResultRowDTOMapper
import fanpoll.infra.database.util.queryDB
import fanpoll.infra.openapi.dynamicQuery
import fanpoll.infra.openapi.post
import fanpoll.infra.openapi.put
import fanpoll.infra.openapi.schema.operation.support.OpenApiModel
import io.konform.validation.Validation
import io.konform.validation.jsonschema.maxLength
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.koin.ktor.ext.inject
import java.time.Instant
import java.util.*

fun Routing.clubUser() {

    val clubUserService by inject<ClubUserService>()

    route("${ClubConst.urlRootPath}/users") {

        authorize(ClubAuth.Root) {

            post<CreateUserForm, UUID>(ClubOpenApi.CreateUser) { form ->
                val id = clubUserService.createUser(form)
                call.respond(DataResponseDTO.uuid(id))
            }
        }

        authorize(ClubAuth.Admin) {

            put<UUIDEntityIdLocation, UpdateUserForm, Unit>(ClubOpenApi.UpdateUser) { _, form ->
                clubUserService.updateUser(form)
                call.respond(HttpStatusCode.OK)
            }

            dynamicQuery<UserDTO>(ClubOpenApi.FindUsers) { dynamicQuery ->
                call.respond(dynamicQuery.queryDB<UserDTO>())
            }
        }

        authorize(ClubAuth.User) {

            put<UpdateUserPasswordForm, Unit>("/myPassword", ClubOpenApi.UpdateMyPassword) { form ->
                val userId = call.principal<UserPrincipal>()!!.userId
                clubUserService.updatePassword(userId, form)
                call.respond(CodeResponseDTO.OK)
            }
        }
    }
}


class ClubUserService {

    fun createUser(form: CreateUserForm): UUID {
        form.password = UserPasswordUtils.hashPassword(form.password)
        return transaction {
            if (ClubUserTable.select { ClubUserTable.account eq form.account }.count() > 0)
                throw RequestException(InfraResponseCode.ENTITY_ALREADY_EXISTS, "${form.account} already exists")
            ClubUserTable.insert(form) as UUID
        }
    }

    fun updateUser(form: UpdateUserForm) {
        transaction {
            ClubUserTable.update(form)
        }
    }

    fun updatePassword(userId: UUID, form: UpdateUserPasswordForm) {
        transaction {
            val hashedPassword = ClubUserTable.slice(ClubUserTable.password).select { ClubUserTable.id eq userId }
                .singleOrNull()?.let { it[ClubUserTable.password] }
                ?: throw RequestException(InfraResponseCode.ENTITY_NOT_EXIST, "user $userId does not exist")
            if (UserPasswordUtils.verifyPassword(form.oldPassword, hashedPassword)) {
                ClubUserTable.update({ ClubUserTable.id eq userId }) {
                    it[password] = UserPasswordUtils.hashPassword(form.newPassword)
                }
            } else throw RequestException(InfraResponseCode.AUTH_BAD_PASSWORD)
        }
    }
}

@OpenApiModel(propertyNameOrder = ["account", "password", "enabled", "role", "name"])
@Serializable
data class CreateUserForm(
    val account: String,
    var password: String,
    val enabled: Boolean = true,
    val role: ClubUserRole,
    val name: String,
    val gender: Gender? = null,
    val birthYear: Int? = null,
    val email: String? = null,
    val mobile: String? = null,
    val lang: Lang? = null
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
            CreateUserForm::email ifPresent { run(ValidationUtils.EMAIL_VALIDATOR) }
            CreateUserForm::mobile ifPresent { run(ValidationUtils.MOBILE_NUMBER_VALIDATOR) }
        }
    }
}

@OpenApiModel(propertyNameOrder = ["id", "enabled", "role", "name"])
@Serializable
data class UpdateUserForm(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    val enabled: Boolean? = null,
    val role: ClubUserRole? = null,
    val name: String? = null,
    val gender: Gender? = null,
    val birthYear: Int? = null,
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
            UpdateUserForm::mobile ifPresent { run(ValidationUtils.MOBILE_NUMBER_VALIDATOR) }
        }
    }
}

@OpenApiModel(propertyNameOrder = ["id", "account", "enabled", "role", "name"])
@Serializable
data class UserDTO(@JvmField @Serializable(with = UUIDSerializer::class) val id: UUID) : EntityDTO<UUID> {

    var account: String? = null
    var enabled: Boolean? = null
    var role: ClubUserRole? = null

    var name: String? = null
    var gender: Gender? = null

    var birthYear: Int? = null

    var email: String? = null
    var mobile: String? = null
    var lang: Lang? = null

    @Transient
    var password: String? = null

    @Serializable(with = InstantSerializer::class)
    var createdAt: Instant? = null

    var devices: List<UserDeviceDTO>? = null

    override fun getId(): UUID = id

    companion object {
        val mapper: ResultRowDTOMapper<UserDTO> = ResultRowDTOMapper(
            UserDTO::class, ClubUserTable,
            joins = listOf(DynamicDBJoinPart(JoinType.LEFT, UserDeviceTable, ClubUserTable.id, UserDeviceTable.userId))
        )
    }
}

enum class Gender {
    Male, Female
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

object ClubUserTable : UUIDTable(name = "club_user") {

    val account = varchar("account", ValidationUtils.EMAIL_MAX_LENGTH) //unique
    val enabled = bool("enabled")
    val role = enumerationByName("role", 20, ClubUserRole::class)

    val name = varchar("name", USER_NAME_LENGTH)
    val gender = enumeration("gender", Gender::class).nullable()
    val birthYear = integer("birth_year").nullable()
    val email = varchar("email", ValidationUtils.EMAIL_MAX_LENGTH).nullable()
    val mobile = varchar("mobile", ValidationUtils.MOBILE_NUMBER_LENGTH).nullable()
    val lang = lang("lang").nullable()

    val password = varchar("password", 1000)
    val createdAt = timestamp("created_at")
        .defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentTimestamp())
    val updatedAt = timestamp("updated_at")
        .defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentTimestamp())

    override val naturalKeys: List<Column<out Any>> = listOf(account)
    override val surrogateKey: Column<EntityID<UUID>> = id
}

private const val USER_NAME_LENGTH = 30