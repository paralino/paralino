package app.paralino.android.ui.navigation

import app.paralino.core.presentation.login.LoginScreenUiType

sealed class AppScreen {

    val argumentGroupId: String = "groupId"

    abstract val routeSignature: String

    object AppStartRoot : AppScreen() {
        override val routeSignature: String = "appStart"

        object Welcome : AppScreen() {
            override val routeSignature: String = "welcome"
        }

        object Login : AppScreen() {

            val argumentStartScreenType: String = "startScreenType"

            override val routeSignature: String =
                "login?$argumentStartScreenType={$argumentStartScreenType}"

            fun route(screenUiType: LoginScreenUiType? = null): String {
                if (screenUiType == null)
                    return "login"
                return "login?$argumentStartScreenType=${screenUiType.name}"
            }
        }

        object ProfileSetup : AppScreen() {
            override val routeSignature: String = "setup-profile"
        }

        object AccountRecovery : AppScreen() {
            override val routeSignature: String = "recover-account"
        }

        object PasswordReset : AppScreen() {
            val argumentUserId: String = Deeplink.PasswordReset.argumentUserId
            val argumentSecret: String = Deeplink.PasswordReset.argumentSecret
            val argumentExpire: String = Deeplink.PasswordReset.argumentExpire
            override val routeSignature: String = Deeplink.PasswordReset.routeSignature()

            fun route(deeplink: Deeplink.PasswordReset): String {
                return deeplink.route()
            }
        }
    }

    object DashboardRoot : AppScreen() {
        override val routeSignature: String = "home"

        object Dashboard : AppScreen() {
            override val routeSignature: String = "home-dashboard"
        }

        object SharingStart : AppScreen() {
            override val routeSignature: String = "home-share?$argumentGroupId={$argumentGroupId}"
            fun route(groupId: String? = null): String {
                if (groupId == null)
                    return "home-share"
                return "home-share?$argumentGroupId=$groupId"
            }
        }

        object GroupDetails : AppScreen() {
            override val routeSignature: String = "home-group/{$argumentGroupId}"
            fun route(groupId: String): String {
                return "home-group/$groupId"
            }
        }

        object GroupCreate : AppScreen() {
            override val routeSignature: String = "group-create?$argumentGroupId={$argumentGroupId}"
            fun route(groupId: String? = null): String {
                if (groupId == null)
                    return "group-create"
                return "group-create?$argumentGroupId=$groupId"
            }
        }

        object GroupMemberAdd : AppScreen() {
            val argumentCode: String = "code"
            override val routeSignature: String =
                "group-member-add?$argumentGroupId={$argumentGroupId}&$argumentCode={$argumentCode}"

            fun route(groupId: String? = null, code: String? = null): String {
                val args = arrayOf(
                    groupId?.let { "$argumentGroupId=$groupId" },
                    code?.let { "$argumentCode=$code" }
                )
                    .filterNotNull()
                    .joinToString(separator = "&")

                if (args.isEmpty())
                    return "group-member-add"
                return "group-member-add?$args"
            }
        }
    }

    object MapRoot : AppScreen() {
        override val routeSignature: String = "map"

        object MainMap : AppScreen() {
            override val routeSignature: String = "map-mainMap?$argumentGroupId={$argumentGroupId}"
            fun route(groupId: String? = null): String {
                if (groupId == null)
                    return "map-mainMap"
                return "map-mainMap?$argumentGroupId=$groupId"
            }
        }
    }

    object SettingsRoot : AppScreen() {
        override val routeSignature: String = "settings"

        object Settings : AppScreen() {
            override val routeSignature: String = "settings-main"
        }

        object PersonalCodes : AppScreen() {
            val argumentCreateCode: String = "createCode"
            override val routeSignature: String = "personal-codes?$argumentCreateCode={$argumentCreateCode}"
            fun route(createCode: Boolean = false): String {
                return "personal-codes?$argumentCreateCode=$createCode"
            }
        }

        object ProfileDetails : AppScreen() {
            override val routeSignature: String = "profile"

            object EditProfile : AppScreen() {
                override val routeSignature: String = "profile-edit"
            }

            object ChangeEmail : AppScreen() {
                override val routeSignature: String = "change-email"
            }

            object ChangePassword : AppScreen() {
                override val routeSignature: String = "change-password"
            }

            object ConvertAccount : AppScreen() {
                override val routeSignature: String = "convert-account"
            }
        }
    }
}