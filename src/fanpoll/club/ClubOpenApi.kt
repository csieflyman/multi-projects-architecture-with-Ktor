/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.club

import fanpoll.infra.ResponseCode
import fanpoll.infra.auth.UserType
import fanpoll.infra.notification.NotificationType
import fanpoll.infra.openapi.definition.*
import fanpoll.infra.openapi.definition.ComponentsUtils.createErrorResponseDef
import fanpoll.infra.openapi.definition.DefaultReusableComponents.ErrorResponseSchema
import fanpoll.infra.openapi.support.OpenApiRoute
import fanpoll.infra.openapi.support.OpenApiRouteSupport
import kotlin.reflect.full.memberProperties

object ClubOpenApiRoutes {

    private val AuthTag = Tag("auth", "登入、登出、變更密碼")
    private val UserTag = Tag("user", "使用者帳號")
    private val ClubTag = Tag("club", "")
    private val NotificationTag = Tag("notification", "通知(推播)")
    private val DataTag = Tag("data")

    val CreateUser = OpenApiRoute("CreateUser", listOf(UserTag))
    val UpdateUser = OpenApiRoute("UpdateUser", listOf(UserTag))
    val FindUsers = OpenApiRoute("FindUsers", listOf(UserTag))
    val UpdateMyPassword = OpenApiRoute("UpdateMyPassword", listOf(AuthTag))

    val Login = OpenApiRoute("Login", listOf(AuthTag)) {
        setErrorResponse(
            createErrorResponseDef(
                setOf(
                    ResponseCode.AUTH_PRINCIPAL_DISABLED,
                    ResponseCode.AUTH_TENANT_DISABLED,
                    ResponseCode.AUTH_LOGIN_UNAUTHENTICATED
                ), ErrorResponseSchema
            )
        )
    }
    val Logout = OpenApiRoute("Logout", listOf(AuthTag))

    val PushNotification = OpenApiRoute("PushNotification", listOf(NotificationTag))
    val DynamicReport = OpenApiRoute("DynamicReport", listOf(DataTag))

    fun all(): List<OpenApiRoute> = ClubOpenApiRoutes::class.memberProperties
        .filter { it.returnType == OpenApiRouteSupport.routeType }
        .map { it.getter.call(this) as OpenApiRoute }
}

object ClubReusableComponents : ReusableComponents {

    private val UserTypeSchema = PropertyDef(
        UserType::class.simpleName!!, SchemaDataType.string,
        kClass = UserType::class
    )

    private val NotificationTypeSchema = PropertyDef(
        NotificationType::class.simpleName!!, SchemaDataType.string,
        kClass = NotificationType::class
    )

    override fun loadReferenceObjects(): List<ReferenceObject> {
        UserTypeSchema.enum = UserType.values().filter { it.projectId == ClubConst.projectId }.map { it.id }
        NotificationTypeSchema.enum = NotificationType.values().filter { it.projectId == ClubConst.projectId }.map { it.id }
        return listOf(UserTypeSchema, NotificationTypeSchema).map { it.definition.createRef() }
    }
}

