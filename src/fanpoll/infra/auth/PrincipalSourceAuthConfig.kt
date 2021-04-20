/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */
package fanpoll.infra.auth

import fanpoll.infra.utils.ConfigUtils
import fanpoll.infra.utils.IdentifiableObject
import fanpoll.infra.utils.MyConfig
import io.ktor.auth.Credential
import java.time.Duration

abstract class PrincipalSourceAuthConfig<T : Credential> : IdentifiableObject<String>() {

    abstract fun authenticate(credential: T): ServicePrincipal?
}

data class ServiceHostAuthConfig(
    override val id: String,
    private val allowHosts: String,
) : PrincipalSourceAuthConfig<ServiceAuthHostCredential>() {

    override fun authenticate(credential: ServiceAuthHostCredential): ServicePrincipal? {
        val allow = when (allowHosts) {
            "" -> false
            "*" -> true
            else -> allowHosts.split(",").any { it == credential.host }
        }
        return if (allow) ServicePrincipal(PrincipalSource.lookup(id), ServiceRole.Private) else null
    }
}

data class ServiceAuthConfig(
    override val id: String,
    private val privateKey: String,
) : PrincipalSourceAuthConfig<ServiceAuthApiKeyCredential>() {

    override fun authenticate(credential: ServiceAuthApiKeyCredential): ServicePrincipal? = when (credential.apiKey) {
        privateKey -> ServicePrincipal(PrincipalSource.lookup(id), ServiceRole.Private)
        else -> null
    }
}

data class UserAuthConfig(
    override val id: String,
    val publicKey: String,
    val runAsKey: String? = null,
    var session: SessionConfig? = null,
    private val privateKey: String? = null,
) : PrincipalSourceAuthConfig<ServiceAuthApiKeyCredential>() {

    override fun authenticate(credential: ServiceAuthApiKeyCredential): ServicePrincipal? = when (credential.apiKey) {
        privateKey -> ServicePrincipal(PrincipalSource.lookup(id), ServiceRole.Private)
        publicKey -> ServicePrincipal(PrincipalSource.lookup(id), ServiceRole.Public)
        else -> null
    }
}

data class SessionConfig(val expireDuration: Duration?, val extendDuration: Duration?) : MyConfig {

    override fun validate() {
        ConfigUtils.require(if (expireDuration != null && extendDuration != null) expireDuration > extendDuration else true) {
            "expireDuration $expireDuration should be larger than extendDuration $extendDuration"
        }
    }
}