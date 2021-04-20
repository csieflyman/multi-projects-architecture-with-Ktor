/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.login

import fanpoll.infra.app.AppOs
import fanpoll.infra.app.CreateUserDeviceForm
import fanpoll.infra.app.UpdateUserDeviceForm
import fanpoll.infra.auth.*
import fanpoll.infra.controller.Form
import fanpoll.infra.controller.ValidationUtils
import fanpoll.infra.controller.publicRemoteHost
import fanpoll.infra.utils.UUIDSerializer
import io.konform.validation.Validation
import io.ktor.application.ApplicationCall
import io.ktor.auth.principal
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

data class LoginResult(val code: LoginResultCode, val userPrincipal: UserPrincipal? = null)

enum class LoginResultCode {
    SUCCESS, ACCOUNT_NOT_FOUND, BAD_CREDENTIAL, ACCOUNT_DISABLED, TENANT_DISABLED, OAUTH_NEW_USER
}

@Serializable
abstract class LoginForm<T> : Form<T>() {

    abstract val account: String
    abstract val password: String
    abstract val tenantId: String?

    var clientVersion: String? = null

    abstract val deviceId: UUID?
    abstract val devicePushToken: String?

    @Transient
    lateinit var source: PrincipalSource

    @Transient
    var checkClientVersion: Boolean = false

    @Transient
    var ip: String? = null

    @Transient
    lateinit var userType: UserType

    @Transient
    lateinit var userId: UUID

    @Transient
    var userRoles: Set<UserRole>? = null

    open fun populateRequest(call: ApplicationCall) {
        source = call.principal<ServicePrincipal>()!!.source
        ip = call.request.publicRemoteHost

        checkClientVersion = clientVersion != null && source.checkClientVersion &&
                !call.request.headers.contains(HEADER_CLIENT_VERSION)
        if (checkClientVersion)
            call.attributes.put(ATTRIBUTE_KEY_CLIENT_VERSION, clientVersion!!)
    }

    fun populateUser(userType: UserType, userId: UUID, userRoles: Set<UserRole>? = null) {
        this.userType = userType
        this.userId = userId
        this.userRoles = userRoles
    }
}

@Serializable
class WebLoginForm(
    override val account: String, override val password: String,
    override val tenantId: String? = null,
    @Serializable(with = UUIDSerializer::class) override val deviceId: UUID?, // UserDevice feature is optional for web user
    override val devicePushToken: String? = null,
    private val userAgent: String? = null
) : LoginForm<WebLoginForm>() {

    override fun validator(): Validation<WebLoginForm> = VALIDATOR

    companion object {
        private val VALIDATOR: Validation<WebLoginForm> = Validation {
            WebLoginForm::account required { run(ValidationUtils.EMAIL_VALIDATOR) }
            WebLoginForm::password required { run(ValidationUtils.PASSWORD_VALIDATOR) }
        }
    }
}

// API_VERSION => deviceId, deviceOsVersion 改為 not null
@Serializable
class AppLoginForm(
    override val account: String, override val password: String,
    override val tenantId: String? = null,
    @Serializable(with = UUIDSerializer::class) override val deviceId: UUID? = null,
    override val devicePushToken: String? = null, // iOs user may disallow permission
    private val deviceOsVersion: String? = null
) : LoginForm<AppLoginForm>() {

    @Transient
    lateinit var appOs: AppOs

    fun toCreateUserDeviceDTO(userType: UserType, userId: UUID): CreateUserDeviceForm =
        CreateUserDeviceForm(deviceId!!, userType, userId, appOs.toUserDeviceType(), devicePushToken, deviceOsVersion)

    fun toUpdateUserDeviceDTO(): UpdateUserDeviceForm = UpdateUserDeviceForm(deviceId!!, devicePushToken, deviceOsVersion)

    override fun populateRequest(call: ApplicationCall) {
        super.populateRequest(call)
        appOs = call.principal<ServicePrincipal>()!!.source.userDeviceType!!.let { AppOs.from(it) }
    }

    override fun validator(): Validation<AppLoginForm> = VALIDATOR

    companion object {
        private val VALIDATOR: Validation<AppLoginForm> = Validation {
            AppLoginForm::account required { run(ValidationUtils.EMAIL_VALIDATOR) }
            AppLoginForm::password required { run(ValidationUtils.PASSWORD_VALIDATOR) }
        }
    }
}

@Serializable
data class LoginResponse(val sid: String? = null, var clientVersionCheckResult: ClientVersionCheckResult? = null)