package app.paralino.android.ui.navigation

import app.paralino.core.presentation.login.LoginScreenUiType
import app.paralino.core.presentation.rawdata.RawPresentationDataType
import app.paralino.core.ui.settings.BillingScreenSourceType

sealed class AppScreen {

    val argumentGroupId: String = "groupId"

    abstract val routeSignature: String

    object LoggedOut : AppScreen() {
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

    object MainHome : AppScreen() {
        override val routeSignature: String = "home"

        object GroupConnect : AppScreen() {
            override val routeSignature: String = "home-connect"
        }

        object MainMap : AppScreen() {
            override val routeSignature: String = "map-mainMap?$argumentGroupId={$argumentGroupId}"
            fun route(groupId: String? = null): String {
                if (groupId == null)
                    return "map-mainMap"
                return "map-mainMap?$argumentGroupId=$groupId"
            }
        }

        object SharingStart : AppScreen() {
            val argumentOnboarding: String = "onboarding"
            override val routeSignature: String = "home-share?$argumentGroupId={$argumentGroupId}&$argumentOnboarding={$argumentOnboarding}"
            fun route(groupId: String? = null, isOnboarding: Boolean = false): String {
                val args = arrayOf(
                    groupId?.let { "$argumentGroupId=$groupId" },
                    "${argumentOnboarding}=$isOnboarding",
                )
                    .filterNotNull()
                    .joinToString(separator = "&")

                if (args.isEmpty())
                    return "home-share"
                return "home-share?$args"
            }
        }

        object GroupDetails : AppScreen() {
            override val routeSignature: String = "home-group/{$argumentGroupId}"
            fun route(groupId: String): String {
                return "home-group/$groupId"
            }
        }

        object GroupSettings : AppScreen() {
            override val routeSignature: String = "group-settings/{$argumentGroupId}"
            fun route(groupId: String): String {
                return "group-settings/$groupId"
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
            val argumentOnboarding: String = "onboarding"
            override val routeSignature: String =
                "group-member-add?$argumentGroupId={$argumentGroupId}&$argumentCode={$argumentCode}&$argumentOnboarding={$argumentOnboarding}"

            fun route(groupId: String? = null, code: String? = null, isOnboarding: Boolean = false): String {
                val args = arrayOf(
                    groupId?.let { "$argumentGroupId=$groupId" },
                    code?.let { "$argumentCode=$code" },
                    "$argumentOnboarding=$isOnboarding",
                )
                    .filterNotNull()
                    .joinToString(separator = "&")

                if (args.isEmpty())
                    return "group-member-add"
                return "group-member-add?$args"
            }
        }

        object PlacesList : AppScreen() {
            val argumentOnboarding: String = "onboarding"
            override val routeSignature: String = "places-list?$argumentGroupId={$argumentGroupId}&${argumentOnboarding}={$argumentOnboarding}"
            fun route(groupId: String? = null, isOnboarding: Boolean = false): String {
                val args = arrayOf(
                    groupId?.let { "$argumentGroupId=$groupId" },
                    "${argumentOnboarding}=$isOnboarding",
                )
                    .filterNotNull()
                    .joinToString(separator = "&")

                if (args.isEmpty())
                    return "places-list"
                return "places-list?$args"
            }
        }

        object PlaceCreate : AppScreen() {
            val argumentPlaceId: String = "placeId"
            val argumentLat: String = "lat"
            val argumentLng: String = "lng"
            override val routeSignature: String =
                "places-create?$argumentGroupId={$argumentGroupId}&$argumentPlaceId={$argumentPlaceId}&$argumentLat={$argumentLat}&$argumentLng={$argumentLng}"

            fun route(
                groupId: String,
                placeId: String?,
                lat: Double? = null,
                lng: Double? = null
            ): String {
                val args = arrayOf(
                    "$argumentGroupId=$groupId",
                    placeId?.let { "$argumentPlaceId=$placeId" },
                    lat?.let { "$argumentLat=$lat" },
                    lng?.let { "$argumentLng=$lng" }
                )
                    .filterNotNull()
                    .joinToString(separator = "&")
                return "places-create?$args"
            }
        }

        object PlaceDetail : Common.PlaceDetail("home")

        object RawDataScreen : Common.RawDataScreen("home")

        object Subscriptions : Common.Subscriptions("home")
        object BillingPaywall : Common.BillingPaywall("home")
        object FreePlanNoticeScreen : AppScreen() {
            override val routeSignature: String = "free-plan-notice?$argumentGroupId={$argumentGroupId}"
            fun route(groupId: String? = null): String {
                val args = arrayOf(
                    groupId?.let { "$argumentGroupId=$groupId" }
                )
                    .filterNotNull()
                    .joinToString(separator = "&")

                if (args.isEmpty())
                    return "free-plan-notice"
                return "free-plan-notice?$args"
            }
        }
    }

    object Settings : AppScreen() {
        override val routeSignature: String = "settings"

        object SettingsRoot : AppScreen() {
            override val routeSignature: String = "settings-main"
        }

        object PersonalCodes : AppScreen() {
            override val routeSignature: String = "personal-codes"
        }

        object BlockList : AppScreen() {
            override val routeSignature: String = "blocklist"
        }

        object Subscriptions : Common.Subscriptions("settings")
        object BillingPaywall : Common.BillingPaywall("settings")

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

            object RawDataScreen : Common.RawDataScreen("profile")
        }

        object Licences : AppScreen() {
            override val routeSignature: String = "licences"
        }

        object AppStatusDebug : AppScreen() {
            override val routeSignature: String = "app-status"
        }
    }

    object Common {
        open class RawDataScreen(private val parent: String) : AppScreen() {
            val argumentDataType: String = "dataType"
            val argumentDataId: String = "dataId"
            override val routeSignature: String =
                "${parent}-raw-data-screen?$argumentDataType={$argumentDataType}&$argumentDataId={$argumentDataId}"

            fun route(type: RawPresentationDataType): String {

                val id = when (type) {
                    is RawPresentationDataType.GroupMemberPresentation -> type.memberId
                    is RawPresentationDataType.GroupPlacePresentation -> type.placeId
                    is RawPresentationDataType.GroupPresentation -> type.groupId
                    RawPresentationDataType.UserPresentation -> ""
                }

                val args = arrayOf(
                    "$argumentDataType=${type.key}",
                    "$argumentDataId=$id"
                ).joinToString(separator = "&")

                return "${parent}-raw-data-screen?$args"
            }
        }

        open class PlaceDetail(private val parent: String) : AppScreen() {
            val argumentPlaceId: String = "placeId"
            override val routeSignature: String =
                "$parent-places-detail?$argumentGroupId={$argumentGroupId}&$argumentPlaceId={$argumentPlaceId}"

            fun route(groupId: String, placeId: String): String {
                val args = arrayOf(
                    "$argumentGroupId=$groupId",
                    "${argumentPlaceId}=$placeId"
                )
                    .joinToString(separator = "&")
                return "$parent-places-detail?$args"
            }
        }

        open class Subscriptions(private val parent: String) : AppScreen() {
            override val routeSignature: String = "$parent-subscriptions-manage"
            fun route(): String {
                return "$parent-subscriptions-manage"
            }
        }

        open class BillingPaywall(private val parent: String) : AppScreen() {
            val argumentSourceType: String = "type"
            override val routeSignature: String = "$parent-subscriptions-paywall?$argumentGroupId={$argumentGroupId}&$argumentSourceType={$argumentSourceType}"
            fun route(groupId: String? = null, type: BillingScreenSourceType = BillingScreenSourceType.NORMAL): String {
                val args = arrayOf(
                    groupId?.let { "$argumentGroupId=$groupId" },
                    "$argumentSourceType=${type.key}"
                )
                    .filterNotNull()
                    .joinToString(separator = "&")

                if (args.isEmpty())
                    return "$parent-subscriptions-paywall"
                return "$parent-subscriptions-paywall?$args"
            }
        }
    }
}