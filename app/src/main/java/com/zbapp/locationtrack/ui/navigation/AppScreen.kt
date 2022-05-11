package com.zbapp.locationtrack.ui.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.navArgument

sealed class AppScreen {
    val deeplinkScheme: String = "myApp"
    val deeplinkHost: String = "myApp"
    val argumentGroupId: String = "groupId"

    abstract val routeSignature: String

    object Login : AppScreen() {
        override val routeSignature: String = "login"
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
            val arguments: List<NamedNavArgument> = listOf(navArgument(argumentGroupId) { nullable = true })
            fun route(groupId: String? = null): String {
                if (groupId == null)
                    return "group-create"
                return "group-create?$argumentGroupId=$groupId"
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

        object InviteCodes : AppScreen() {
            override val routeSignature: String = "invites"
        }

        object ProfileDetails : AppScreen() {
            override val routeSignature: String = "profile"
        }
    }

    companion object {
//            fun fromRoute(route: String?): MainAppScreen =
//                when (route?.substringBefore("/")) {
//                    Dashboard::class.simpleName -> Dashboard
//                    MainMap::class.simpleName -> MainMap(groupId = null)
//                    Settings::class.simpleName -> Settings
//                    InviteCodes::class.simpleName -> Settings
//                    ProfileDetails::class.simpleName -> Settings
//                    GroupDetails::class.simpleName -> Dashboard
//                    SharingStart::class.simpleName -> Dashboard
//
//                    null -> Dashboard
//                    else -> throw IllegalArgumentException("Route $route is not recognized.")
//                }
    }
}