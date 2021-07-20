/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.login

import fanpoll.infra.app.AppOs
import fanpoll.infra.app.CreateUserDeviceForm
import fanpoll.infra.app.UpdateUserDeviceForm
import fanpoll.infra.app.UserDeviceDTO
import fanpoll.infra.auth.ATTRIBUTE_KEY_CLIENT_VERSION
import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.auth.principal.ServicePrincipal
import fanpoll.infra.auth.principal.UserRole
import fanpoll.infra.auth.principal.UserType
import fanpoll.infra.base.extension.publicRemoteHost
import fanpoll.infra.base.form.Form
import fanpoll.infra.base.form.ValidationUtils
import fanpoll.infra.base.json.UUIDSerializer
import fanpoll.infra.base.tenant.TenantId
import io.konform.validation.Validation
import io.ktor.application.ApplicationCall
import io.ktor.auth.principal
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

@Serializable
abstract class LoginForm<T> : Form<T>() {

    abstract val account: String
    abstract val password: String
    abstract val tenantId: TenantId?

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

        if (clientVersion != null) {
            call.attributes.put(ATTRIBUTE_KEY_CLIENT_VERSION, clientVersion!!)
        } else {
            clientVersion = call.attributes.getOrNull(ATTRIBUTE_KEY_CLIENT_VERSION)
        }
        checkClientVersion = clientVersion != null && source.checkClientVersion()
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
    override val tenantId: TenantId? = null,
    @Serializable(with = UUIDSerializer::class) override val deviceId: UUID? = null,
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

@Serializable
class AppLoginForm(
    override val account: String, override val password: String,
    override val tenantId: TenantId? = null,
    @Serializable(with = UUIDSerializer::class) override val deviceId: UUID,
    override val devicePushToken: String,
    private val deviceOsVersion: String
) : LoginForm<AppLoginForm>() {

    @Transient
    lateinit var appOs: AppOs

    @Transient
    var userDevice: UserDeviceDTO? = null

    fun toCreateUserDeviceDTO(): CreateUserDeviceForm =
        CreateUserDeviceForm(deviceId, userType, userId, appOs.principalType(), devicePushToken, deviceOsVersion)

    fun toUpdateUserDeviceDTO(): UpdateUserDeviceForm = UpdateUserDeviceForm(deviceId, devicePushToken, deviceOsVersion)

    override fun populateRequest(call: ApplicationCall) {
        super.populateRequest(call)
        appOs = call.principal<ServicePrincipal>()!!.source.type.let { AppOs.from(it) }
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
class LogoutForm(val clientVersion: String? = null) : Form<LogoutForm>() {

    @Transient
    var ip: String? = null

    fun populateRequest(call: ApplicationCall) {
        ip = call.request.publicRemoteHost
    }
}