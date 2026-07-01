package app.paralino.core.presentation.privacyzones.create

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import app.paralino.core.MR
import app.paralino.core.models.PlaceZoneExpirationStatus
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.location.LatLng
import app.paralino.core.models.settings.MapsSDKProvider
import app.paralino.core.presentation.SuggestionSectionChipData
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.common.GroupMemberItemStateWithOneLocation
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.presentation.common.ScreenDimensionType
import app.paralino.core.presentation.map.MapMarkerStyle
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.map.MapPinType
import app.paralino.core.presentation.places.create.PlaceAddressSearchUiMode
import app.paralino.core.presentation.places.create.PlaceAddressSuggestion
import app.paralino.core.presentation.places.create.PlaceMapFallbackLatLng
import app.paralino.core.provider.current
import app.paralino.core.provider.localizedString
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.annotatedBold

data class CreatePrivacyZoneScreenState constructor(
    val groupId: GroupOrSpaceId,
    val isNewZone: Boolean,
    val screenStep: CreatePrivacyZoneScreenStep,
    val editOnlyCurrentStep: Boolean,
    val name: String,
    val image: ImageModel,
    val nameSuggestions: List<SuggestionSectionChipData>,
    val address: TextFieldValue,
    val addressUiMode: PlaceAddressSearchUiMode,
    val addressSuggestions: List<PlaceAddressSuggestion>,
    val isAddressLoading: Boolean,
    val fallbackCenterLatLng: PlaceMapFallbackLatLng?,
    val lat: Double?,
    val lng: Double?,
    val radiusPresets: List<PrivacyZoneLocationDistancePair>,
    val radiusValue: PrivacyZoneLocationDistancePair,
    val radiusText: String,
    val hasNameError: Boolean,
    val notifyOthersInZone: Boolean,
    val inZoneMessage: String?,
    val durationPresetsMillis: List<Long>,
    val durationMillis: Long,
    val expirationStatus: PlaceZoneExpirationStatus?,
    val memberPreviewState: GroupMemberItemStateWithOneLocation,
    val isSatelliteModeEnabled: Boolean,
    val actionLoading: Boolean,
    val fullLoading: Boolean,
    val androidMapSDKProvider: MapsSDKProvider?,
    val alertDialog: MainAlertDialog?
) {
    val maxMessageLength: Int = CreatePrivacyZoneScreenState.maxMessageLength
    val deviceType: DeviceType = DeviceType.current
    val hasSatelliteMode: Boolean = when (deviceType) {
        DeviceType.Android -> androidMapSDKProvider?.hasSatelliteMaps != false
        DeviceType.iOS -> true
    }
    val isSatelliteModeActive: Boolean = if (hasSatelliteMode) isSatelliteModeEnabled else false

    val hintText: AnnotatedString = when (addressUiMode) {
        PlaceAddressSearchUiMode.HIDDEN -> {
            when (DeviceType.current) {
                DeviceType.Android -> {
                    MR.strings.privacy_zones_create_location_hint_longpress
                        .localizedString()
                        .annotatedBold()
                }

                DeviceType.iOS -> {
                    MR.strings.privacy_zones_create_location_hint_tap
                        .localizedString()
                        .annotatedBold()
                }
            }
        }

        PlaceAddressSearchUiMode.SEARCH_EXPANDED -> {
            MR.strings.privacy_zones_create_location_hint_searchTap
                .localizedString()
                .annotatedBold()
        }

        PlaceAddressSearchUiMode.FULLY_EXPANDED -> {
            MR.strings.privacy_zones_create_location_hint_searchOnly
                .localizedString()
                .annotatedBold()
        }
    }

    val radiusIndex: Int
        get() {
            return radiusPresets.indexOfFirst { it.meters == radiusValue.meters }
                .takeIf { it >= 0 }
                ?: radiusPresets.indexOfFirst { radiusValue.meters < it.meters }
                    .takeIf { it >= 0 }
                ?: radiusPresets.lastIndex
        }

    val durationIndex: Int
        get() {
            return durationPresetsMillis.indexOfFirst { it == durationMillis }.takeIf { it >= 0 }
                ?: durationPresetsMillis.indexOfFirst { durationMillis < it }.takeIf { it >= 0 }
                ?: durationPresetsMillis.lastIndex
        }

    val showDurationSeekbar: Boolean
        get() = expirationStatus == null

    companion object {
        const val maxMessageLength: Int = 40
        const val maxNameLength: Int = 30
        fun empty(
            groupId: GroupOrSpaceId,
            lat: Double? = null,
            lng: Double? = null,
            radiusValue: PrivacyZoneLocationDistancePair,
            radiusText: String,
            memberPreviewState: GroupMemberItemStateWithOneLocation,
            androidMapSDKProvider: MapsSDKProvider?,
            fallbackCenterLatLng: PlaceMapFallbackLatLng?,
            nameSuggestions: List<SuggestionSectionChipData>
        ): CreatePrivacyZoneScreenState {
            return CreatePrivacyZoneScreenState(
                groupId = groupId,
                isNewZone = true,
                screenStep = CreatePrivacyZoneScreenStep.ADDRESS,
                editOnlyCurrentStep = false,
                name = "",
                image = ImageModel.emoji(Emojis.blackDot),
                nameSuggestions = nameSuggestions,
                address = TextFieldValue(text = ""),
                addressUiMode = if (lat == null || lng == null) {
                    PlaceAddressSearchUiMode.SEARCH_EXPANDED
                } else {
                    PlaceAddressSearchUiMode.HIDDEN
                },
                addressSuggestions = emptyList(),
                isAddressLoading = false,
                fallbackCenterLatLng = fallbackCenterLatLng,
                lat = lat,
                lng = lng,
                radiusPresets = PrivacyZoneLocationDistancePair.pairs,
                radiusValue = radiusValue,
                radiusText = radiusText,
                hasNameError = false,
                notifyOthersInZone = true,
                inZoneMessage = null,
                durationPresetsMillis = PrivacyZoneDurationPreset.presets,
                durationMillis = PrivacyZoneDurationPreset.default,
                expirationStatus = null,
                memberPreviewState = memberPreviewState,
                actionLoading = false,
                fullLoading = false,
                alertDialog = null,
                androidMapSDKProvider = androidMapSDKProvider,
                isSatelliteModeEnabled = false
            )
        }
    }
}

enum class CreatePrivacyZoneScreenStep constructor(val key: String) {
    ADDRESS("address"),
    NAME_SUGGESTION("name_suggestion"),
    SETTINGS("settings");

    companion object {
        fun fromKey(key: String?): CreatePrivacyZoneScreenStep? {
            return entries.firstOrNull { it.key == key }
        }
    }
}

sealed class CreatePrivacyZoneScreenEvent() {
    data object MissingInfo : CreatePrivacyZoneScreenEvent()
    data object RequestKeyboardFocusOnAddressInput : CreatePrivacyZoneScreenEvent()
    data object MissingLocation : CreatePrivacyZoneScreenEvent()
    data object ZoneUpdated : CreatePrivacyZoneScreenEvent()
}

sealed interface CreatePrivacyZoneScreenUiEvent {
    data object CreatePrivacyZone : CreatePrivacyZoneScreenUiEvent
    data class OnUiReady(val size: ScreenDimensionType) : CreatePrivacyZoneScreenUiEvent
    data class OnMapPositionChanged(val position: LatLng) : CreatePrivacyZoneScreenUiEvent
    data class OnNameChanged(val name: String) : CreatePrivacyZoneScreenUiEvent
    data class OnNotifyOthersInZoneChanged(val enabled: Boolean) : CreatePrivacyZoneScreenUiEvent
    data class OnInZoneMessageChanged(val message: String) : CreatePrivacyZoneScreenUiEvent
    data class OnDurationChanged(val index: Int) : CreatePrivacyZoneScreenUiEvent
    data object OnExpirationEditRequested : CreatePrivacyZoneScreenUiEvent
    data class OnRadiusChanged(val index: Int) : CreatePrivacyZoneScreenUiEvent
    data class OnImageChanged(val image: ImageModel) : CreatePrivacyZoneScreenUiEvent
    data class OnAddressChanged(val address: TextFieldValue) : CreatePrivacyZoneScreenUiEvent
    data class OnNameSuggestionSelected(val suggestion: SuggestionSectionChipData) : CreatePrivacyZoneScreenUiEvent
    data class OnAddressSuggestionSelected(val suggestion: PlaceAddressSuggestion) : CreatePrivacyZoneScreenUiEvent
    data object OnMapModeSwitch : CreatePrivacyZoneScreenUiEvent

    data class ChangeSearchFieldFocus(val isFocused: Boolean) : CreatePrivacyZoneScreenUiEvent
    data object ToggleSearchFieldFocus : CreatePrivacyZoneScreenUiEvent
    data class OnMapTap(val lat: Double, val lng: Double) : CreatePrivacyZoneScreenUiEvent
    data class DialogAction(val action: AlertDialogAction) : CreatePrivacyZoneScreenUiEvent
}

object PrivacyZoneDurationPreset {
    private const val second = 1_000L
    private const val minute = 60 * second
    const val hour = 60 * minute
    private const val day = 24 * hour
    const val infinite = -1L

    val default: Long = 8 * hour
    val presets = listOf(
        30 * minute,
        hour,
        2 * hour,
        4 * hour,
        6 * hour,
        8 * hour,
        12 * hour,
        day,
        2 * day,
        3 * day,
        5 * day,
        7 * day,
        10 * day,
        15 * day,
        20 * day,
        30 * day,
        infinite,
    )
}

fun CreatePrivacyZoneScreenState.makeMapPin(): MapPinData {
    val state = this
    return MapPinData(
        groupOrSpaceId = state.groupId,
        markerId = "privacy_zone_marker",
        pinType = MapPinType.PRIVACY_ZONE,
        markerStyle = MapMarkerStyle.CIRCLE,
        focused = false,
        active = true,
        obsolete = false,
        hidden = false,
        title = state.name,
        image = state.image,
        lat = state.lat ?: 0.0,
        lng = state.lng ?: 0.0,
        accuracy = state.radiusValue.meters.toFloat(),
        isApproximate = false,
        shouldShowApproximateCircle = true
    )
}
