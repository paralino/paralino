package app.paralino.android.ui.navigation

import androidx.navigation3.runtime.NavKey
import app.paralino.core.models.settings.RawPresentationDataType
import app.paralino.core.presentation.billing.BillingScreenSourceType
import app.paralino.core.presentation.login.LoginScreenUiType
import app.paralino.core.presentation.places.create.CreatePlaceScreenStep
import app.paralino.core.presentation.privacyzones.create.CreatePrivacyZoneScreenStep
import app.paralino.core.presentation.share.SharingStartPresentationType
import app.paralino.core.utils.DeeplinkType
import kotlinx.serialization.Serializable

sealed interface AppScreen : NavKey {

    object LoggedOut {

        @Serializable
        data object Welcome : AppScreen

        @Serializable
        data object WelcomeOnboarding : AppScreen

        @Serializable
        data class Login(val screenUiType: LoginScreenUiType? = null) : AppScreen

        @Serializable
        data object ProfileSetup : AppScreen

        @Serializable
        data object EmailVerification : AppScreen

        @Serializable
        data object ChangeEmail : AppScreen

        @Serializable
        data object AccountRecovery : AppScreen

        @Serializable
        data object PasswordConfirmation : AppScreen

        @Serializable
        data class PasswordReset(
            val userId: String,
            val secret: String,
            val expire: String?,
        ) : AppScreen {
            constructor(deeplink: DeeplinkType.PasswordReset) : this(
                userId = deeplink.userId,
                secret = deeplink.secret,
                expire = deeplink.expire
            )
        }
    }

    object MainHome {

        @Serializable
        data class MainMap(val groupId: String? = null) : AppScreen

        @Serializable
        data class SharingStart(
            val groupId: String? = null,
            val presentation: SharingStartPresentationType = SharingStartPresentationType.FULL
        ) : AppScreen

        @Serializable
        data object PersonalSpace : AppScreen

        @Serializable
        data object PersonalSpaceSettings : AppScreen

        @Serializable
        data class GroupDetails(val groupId: String) : AppScreen

        @Serializable
        data class GroupSettings(val groupId: String) : AppScreen

        @Serializable
        data class BatteryNotificationSettings(val groupId: String) : AppScreen

        @Serializable
        data class GroupCreate(val groupId: String? = null) : AppScreen

        @Serializable
        data class GroupJoinWelcome(val groupId: String) : AppScreen

        @Serializable
        data class GroupMemberAdd(
            val groupId: String? = null,
            val code: String? = null,
            val isOnboarding: Boolean = false
        ) : AppScreen

        @Serializable
        data class PlacesList(val groupId: String, val isOnboarding: Boolean = false) : AppScreen

        @Serializable
        data class PrivacyZonesList(val groupId: String, val isOnboarding: Boolean = false) : AppScreen

        @Serializable
        data class PrivacyZoneCreate(
            val groupId: String,
            val zoneId: String?,
            val editStep: CreatePrivacyZoneScreenStep? = null,
            val lat: Double? = null,
            val lng: Double? = null
        ) : AppScreen

        @Serializable
        data class PrivacyZoneDetail(val groupId: String, val zoneId: String) : AppScreen

        @Serializable
        data class PlaceCreate(
            val groupId: String,
            val placeId: String?,
            val editStep: CreatePlaceScreenStep?,
            val lat: Double? = null,
            val lng: Double? = null
        ) : AppScreen

        @Serializable
        data class PlaceDetail(val groupId: String, val placeId: String) : AppScreen

        @Serializable
        data class PlaceAlerts(val groupId: String, val placeId: String) : AppScreen

        @Serializable
        data class PlaceSensitivity(val groupId: String, val placeId: String) : AppScreen

        @Serializable
        data class PlaceImprovements(val groupId: String, val placeId: String) : AppScreen

        @Serializable
        data class FalseLocations(val groupId: String, val placeId: String) : AppScreen

        @Serializable
        data class WifiNetworks(val placeId: String?) : AppScreen

        @Serializable
        data class LocationHistoryDelete(val groupId: String) : AppScreen

        @Serializable
        data class FreePlanNoticeScreen(val groupId: String? = null) : AppScreen
    }

    object Settings {

        @Serializable
        data object SettingsRoot : AppScreen

        @Serializable
        data object PersonalCodes : AppScreen

        @Serializable
        data object BlockList : AppScreen

        @Serializable
        data object UserDevices : AppScreen

        @Serializable
        data object ProfileDetails : AppScreen {

            @Serializable
            data object EditProfile : AppScreen

            @Serializable
            data object ChangeEmail : AppScreen

            @Serializable
            data object ChangePassword : AppScreen

            @Serializable
            data object ConvertAccount : AppScreen
        }

        @Serializable
        data object Licences : AppScreen

        @Serializable
        data object LicenceCode : AppScreen

        @Serializable
        data object AppStatusDebug : AppScreen

        @Serializable
        data object AppStatus : AppScreen

        @Serializable
        data object GeneralInfo : AppScreen

        @Serializable
        data object LocationSettings : AppScreen

        @Serializable
        data object WelcomeOnboarding : AppScreen

        @Serializable
        data object AppSettings : AppScreen

        @Serializable
        data object AppTheme : AppScreen

        @Serializable
        data object MeasurementSystem : AppScreen

        @Serializable
        data object PlaceSearch : AppScreen

        @Serializable
        data object PersonalSpaceFeature : AppScreen

        @Serializable
        data object NotificationSocket : AppScreen

        @Serializable
        data object NotificationGrouping : AppScreen

        @Serializable
        data object MapInteractions : AppScreen

        @Serializable
        data object MapsProvider : AppScreen
    }

    /**
     * Screens opened from both home and settings.
     */
    object Common {

        @Serializable
        data class RawDataScreen(val type: RawPresentationDataType) : AppScreen

        @Serializable
        data object Subscriptions : AppScreen

        @Serializable
        data class BillingPaywall(
            val groupId: String? = null,
            val type: BillingScreenSourceType = BillingScreenSourceType.UPGRADE_DEFAULT
        ) : AppScreen
    }
}
