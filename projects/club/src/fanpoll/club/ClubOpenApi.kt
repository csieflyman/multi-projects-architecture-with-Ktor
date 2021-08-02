/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.club

import fanpoll.infra.auth.ClientVersionCheckResult
import fanpoll.infra.auth.login.AppLoginForm
import fanpoll.infra.auth.login.AppLoginResponse
import fanpoll.infra.auth.principal.UserType
import fanpoll.infra.base.i18n.Lang
import fanpoll.infra.base.response.DataResponseDTO
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.notification.NotificationContent
import fanpoll.infra.notification.NotificationType
import fanpoll.infra.notification.Recipient
import fanpoll.infra.notification.channel.email.EmailContent
import fanpoll.infra.notification.channel.push.PushContent
import fanpoll.infra.notification.channel.sms.SMSContent
import fanpoll.infra.notification.util.SendNotificationForm
import fanpoll.infra.openapi.OpenApiOperation
import fanpoll.infra.openapi.ProjectOpenApi
import fanpoll.infra.openapi.schema.Tag
import fanpoll.infra.openapi.schema.component.support.ComponentLoader
import fanpoll.infra.openapi.schema.operation.definitions.ExampleObject
import fanpoll.infra.openapi.schema.operation.definitions.PropertyDef
import fanpoll.infra.openapi.schema.operation.definitions.ReferenceObject
import fanpoll.infra.openapi.schema.operation.definitions.SchemaDataType
import fanpoll.infra.openapi.schema.operation.support.utils.ResponseUtils
import java.util.*
import kotlin.reflect.full.memberProperties
import kotlin.reflect.typeOf

object ClubOpenApi {

    private val AuthTag = Tag("auth")
    private val UserTag = Tag("user")

    val CreateUser = OpenApiOperation("CreateUser", listOf(UserTag))
    val UpdateUser = OpenApiOperation("UpdateUser", listOf(UserTag))
    val FindUsers = OpenApiOperation("FindUsers", listOf(UserTag))
    val UpdateMyPassword = OpenApiOperation("UpdateMyPassword", listOf(UserTag))
    val SendNotification = OpenApiOperation("SendNotification", listOf(UserTag)) {
        addRequestExample(
            SendNotificationForm(
                recipients = mutableSetOf(Recipient("tester@test.com", name = "tester", email = "tester@test.com")),
                userFilters = mapOf(ClubUserType.User.value to "[account = tester@test.com]"),
                content = NotificationContent(
                    email = mutableMapOf(Lang.zh_TW to EmailContent("Test Email", "This is a test")),
                    push = mutableMapOf(Lang.zh_TW to PushContent("Test Push", "This is a test")),
                    sms = mutableMapOf(Lang.zh_TW to SMSContent("Test SMS"))
                ),
                contentArgs = mutableMapOf("data" to "test")
            )
        )
    }

    val Login = OpenApiOperation("Login", listOf(AuthTag)) {

        addErrorResponses(
            InfraResponseCode.AUTH_PRINCIPAL_DISABLED,
            InfraResponseCode.AUTH_LOGIN_UNAUTHENTICATED
        )

        addRequestExample(
            AppLoginForm(
                "tester@test.com", "test123", null,
                UUID.randomUUID(), "pushToken", "Android 9.0"
            )
        )

        addResponseExample(
            InfraResponseCode.OK,
            ExampleObject(
                ClientVersionCheckResult.Latest.name, ClientVersionCheckResult.Latest.name, "已是最新版本",
                DataResponseDTO(
                    AppLoginResponse(
                        "club:android:user:421feef3-c1b4-4525-a416-6a11cf6ed9ca:2d7674bb47ec1c58681ce56c49ba9e4d",
                        ClientVersionCheckResult.Latest
                    )
                )
            ),
            ExampleObject(
                ClientVersionCheckResult.ForceUpdate.name, ClientVersionCheckResult.ForceUpdate.name, "必須先更新版本才能繼續使用",
                DataResponseDTO(
                    AppLoginResponse(
                        "club:android:user:421feef3-c1b4-4525-a416-6a11cf6ed9ca:2d7674bb47ec1c58681ce56c49ba9e4d",
                        ClientVersionCheckResult.ForceUpdate
                    )
                )
            )
        )
    }
    val Logout = OpenApiOperation("Logout", listOf(AuthTag))

    private val operationType = typeOf<OpenApiOperation>()

    private val allOperations = ClubOpenApi::class.memberProperties
        .filter { it.returnType == operationType }
        .map { it.getter.call(this) as OpenApiOperation }

    private val components = object : ComponentLoader {

        private val UserTypeSchema = PropertyDef(
            UserType::class.simpleName!!, SchemaDataType.string,
            kClass = UserType::class
        )

        private val NotificationTypeSchema = PropertyDef(
            NotificationType::class.simpleName!!, SchemaDataType.string,
            kClass = NotificationType::class
        )

        private val ResponseCodeValueSchema = PropertyDef(
            "ClubResponseCode", SchemaDataType.string,
            ResponseUtils.buildResponseCodesDescription(ClubResponseCode.AllCodes),
            enum = ClubResponseCode.AllCodes.map { it.value }.toList()
        )

        override fun load(): List<ReferenceObject> {
            UserTypeSchema.enum = UserType.values().filter { it.projectId == ClubConst.projectId }.map { it.id }
            NotificationTypeSchema.enum = NotificationType.values().filter { it.projectId == ClubConst.projectId }.map { it.id }
            return listOf(UserTypeSchema, NotificationTypeSchema, ResponseCodeValueSchema).map { it.createRef() }
        }
    }

    val Instance = ProjectOpenApi(
        ClubConst.projectId, ClubConst.urlRootPath, ClubAuth.allAuthSchemes,
        allOperations, components
    )
}

