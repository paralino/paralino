package com.zbapp.locationtrack.ui.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.navArgument

sealed class AppScreen {
    val deeplinkScheme: String = "myApp"
    val deeplinkHost: String = "myApp"
    val argumentGroupId: String = "groupId"

    abstract val routeSignature: String

    object AppStartRoot : AppScreen() {
        override val routeSignature: String = "appStart"
        object Welcome : AppScreen() {
            override val routeSignature: String = "welcome"
        }
        object Login : AppScreen() {
            override val routeSignature: String = "login"
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
            override val routeSignature: String = "group-member-add?$argumentGroupId={$argumentGroupId}&$argumentCode={$argumentCode}"
            fun route(groupId: String? = null, code: String? = null): String {
                val args = arrayOf(groupId?.let { "$argumentGroupId=$groupId" }, code?.let { "$argumentCode=$code" })
                    .filterNotNull().joinToString(separator = "&")
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
            override val routeSignature: String = "personal_codes"
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
        }
    }
}