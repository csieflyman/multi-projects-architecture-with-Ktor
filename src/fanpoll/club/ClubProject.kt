/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.club

import fanpoll.MyApplicationConfig
import fanpoll.club.features.ClubUserTable
import fanpoll.infra.Project
import fanpoll.infra.ProjectConfig
import fanpoll.infra.auth.*
import fanpoll.infra.controller.receiveUTF8Text
import fanpoll.infra.database.myTransaction
import fanpoll.infra.openapi.ProjectOpenApi
import io.ktor.request.path
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.select
import java.util.*

object ClubConst {

    const val projectId = "club"

    const val urlRootPath = "/club"
}

private val openApi = ProjectOpenApi(
    ClubConst.projectId,
    ClubConst.urlRootPath,
    ClubAuth.allAuthSchemes,
    ClubOpenApiOperations.all(),
    ClubComponents
)

val ClubProject = object : Project(
    ClubConst.projectId,
    ClubPrincipalSources.All, ClubUserType.values().map { it.value }.toSet(),
    openApi,
    ClubNotificationTypes.all()
) {

    override fun configure(appConfig: MyApplicationConfig) {
        config = appConfig.club

        filterRequestBodySensitiveData(appConfig)
    }

    private fun filterRequestBodySensitiveData(appConfig: MyApplicationConfig) {
        val requestPaths = mutableListOf("/login", "/myPassword")
            .map { ClubConst.urlRootPath + it }
        appConfig.logging.requestBodySensitiveDataFilter = { call ->
            if (requestPaths.contains(call.request.path())) null
            else runBlocking { call.receiveUTF8Text() }
        }
    }
}

data class ClubConfig(
    override val auth: ClubAuthConfig
) : ProjectConfig

object ClubPrincipalSources {

    private val Android: PrincipalSource = PrincipalSource(
        ClubConst.projectId, "android", true, UserDeviceType.Android
    )
    private val iOS: PrincipalSource = PrincipalSource(
        ClubConst.projectId, "iOS", true, UserDeviceType.iOS
    )

    val App: Set<PrincipalSource> = setOf(Android, iOS)

    val All: Set<PrincipalSource> = setOf(Android, iOS)
}

enum class ClubUserType(val value: UserType) {

    User(object : UserType(ClubConst.projectId, "user") {

        override val roles: Set<UserRole> = setOf(UserRole(id, "admin"), UserRole(id, "member"))

        override fun findRunAsUserById(userId: UUID): fanpoll.infra.auth.User {
            val row = myTransaction {
                ClubUserTable.select { ClubUserTable.id eq userId }.single()
            }
            return User(this, userId, setOf(row[ClubUserTable.role].value))
        }
    })
}

enum class ClubUserRole(val value: UserRole) {

    Admin(ClubUserType.User.value.roles!!.first { it.name == "admin" }),
    Member(ClubUserType.User.value.roles!!.first { it.name == "member" })
}