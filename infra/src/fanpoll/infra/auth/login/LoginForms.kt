/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.login

import fanpoll.infra.auth.principal.ClientAttributeKey
import fanpoll.infra.auth.principal.PrincipalSource
import fanpoll.infra.auth.principal.ServicePrincipal
import fanpoll.infra.auth.principal.UserPrincipal
import fanpoll.infra.base.extension.publicRemoteHost
import fanpoll.infra.base.form.Form
import fanpoll.infra.base.form.ValidationUtils
import fanpoll.infra.logging.RequestAttributeKey
import io.konform.validation.Validation
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
abstract class LoginForm<Self : LoginForm<Self>> : Form<Self>() {

    abstract val account: String
    abstract val password: String
    abstract var clientVersion: String?

    @Transient
    lateinit var source: PrincipalSource

    @Transient
    var checkClientVersion: Boolean = false

    @Transient
    var ip: String? = null

    @Transient
    var traceId: String? = null

    open fun populateRequest(call: ApplicationCall) {
        traceId = call.attributes.getOrNull(RequestAttributeKey.TRACE_ID)
        source = call.principal<ServicePrincipal>()!!.source
        ip = call.request.publicRemoteHost

        if (clientVersion != null) {
            call.attributes.put(ClientAttributeKey.CLIENT_VERSION, clientVersion!!)
        } else {
            clientVersion = call.attributes.getOrNull(ClientAttributeKey.CLIENT_VERSION)
        }
        checkClientVersion = clientVersion != null && source.checkClientVersion
    }
}

@Serializable
class WebLoginForm(
    override val account: String,
    override val password: String,
    override var clientVersion: String? = null
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
class LogoutForm(val clientVersion: String? = null) : Form<LogoutForm>() {

    @Transient
    lateinit var source: PrincipalSource

    @Transient
    var ip: String? = null

    @Transient
    var traceId: String? = null

    fun populateRequest(call: ApplicationCall) {
        traceId = call.attributes.getOrNull(RequestAttributeKey.TRACE_ID)
        source = call.principal<UserPrincipal>()!!.source
        ip = call.request.publicRemoteHost
    }
}