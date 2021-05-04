/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.club

import fanpoll.infra.ResponseCode
import fanpoll.infra.auth.UserType
import fanpoll.infra.notification.NotificationType
import fanpoll.infra.openapi.OpenApiOperation
import fanpoll.infra.openapi.schema.Tag
import fanpoll.infra.openapi.schema.component.support.ComponentLoader
import fanpoll.infra.openapi.schema.operation.definitions.PropertyDef
import fanpoll.infra.openapi.schema.operation.definitions.ReferenceObject
import fanpoll.infra.openapi.schema.operation.definitions.SchemaDataType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.typeOf

object ClubOpenApiOperations {

    private val AuthTag = Tag("auth", "登入、登出、變更密碼")
    private val UserTag = Tag("user", "使用者帳號")
    private val NotificationTag = Tag("notification", "通知(推播)")
    private val DataTag = Tag("data")

    val CreateUser = OpenApiOperation("CreateUser", listOf(UserTag))
    val UpdateUser = OpenApiOperation("UpdateUser", listOf(UserTag))
    val FindUsers = OpenApiOperation("FindUsers", listOf(UserTag))
    val UpdateMyPassword = OpenApiOperation("UpdateMyPassword", listOf(AuthTag))

    val Login = OpenApiOperation("Login", listOf(AuthTag)) {
        addErrorResponses(
            ResponseCode.AUTH_PRINCIPAL_DISABLED,
            ResponseCode.AUTH_LOGIN_UNAUTHENTICATED
        )
    }
    val Logout = OpenApiOperation("Logout", listOf(AuthTag))

    val PushNotification = OpenApiOperation("PushNotification", listOf(NotificationTag))
    val DynamicReport = OpenApiOperation("DynamicReport", listOf(DataTag))

    private val routeType = typeOf<OpenApiOperation>()

    fun all(): List<OpenApiOperation> = ClubOpenApiOperations::class.memberProperties
        .filter { it.returnType == routeType }
        .map { it.getter.call(this) as OpenApiOperation }
}

object ClubComponents : ComponentLoader {

    private val UserTypeSchema = PropertyDef(
        UserType::class.simpleName!!, SchemaDataType.string,
        kClass = UserType::class
    )

    private val NotificationTypeSchema = PropertyDef(
        NotificationType::class.simpleName!!, SchemaDataType.string,
        kClass = NotificationType::class
    )

    override fun load(): List<ReferenceObject> {
        UserTypeSchema.enum = UserType.values().filter { it.projectId == ClubConst.projectId }.map { it.id }
        NotificationTypeSchema.enum = NotificationType.values().filter { it.projectId == ClubConst.projectId }.map { it.id }
        return listOf(UserTypeSchema, NotificationTypeSchema).map { it.createRef() }
    }
}

