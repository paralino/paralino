package app.paralino.core.presentation.places.create

import androidx.compose.ui.text.AnnotatedString
import app.paralino.core.MR
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.location.LatLng
import app.paralino.core.provider.current
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.groups.components.SuggestionSectionChipData
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.annotatedBold

data class CreatePlaceScreenState constructor(
    val groupId: GroupOrSpaceId,
    val isNewPlace: Boolean,
    val screenStep: CreatePlaceScreenStep,
    val name: String,
    val image: ImageModel,
    val nameSuggestions: List<SuggestionSectionChipData>,
    val address: String,
    val addressUiMode: CreatePlaceAddressSearchUiMode,
    val addressSuggestions: List<PlaceAddressSuggestion>,
    val isAddressLoading: Boolean,
    val private: Boolean,
    val lat: Double?,
    val lng: Double?,
    val radiusMeters: Int,
    val radiusText: String,
    val hasNameError: Boolean,
    val actionLoading: Boolean,
    val fullLoading: Boolean,
    val alertDialog: MainAlertDialog?
) {
    val hintText: AnnotatedString = when (addressUiMode) {
        CreatePlaceAddressSearchUiMode.HIDDEN -> {
            when (DeviceType.current) {
                DeviceType.Android -> {
                    MR.strings.places_create_location_hint_longpress
                        .localizedString()
                        .annotatedBold()
                }

                DeviceType.iOS -> {
                    MR.strings.places_create_location_hint_tap
                        .localizedString()
                        .annotatedBold()
                }
            }
        }

        CreatePlaceAddressSearchUiMode.SEARCH_EXPANDED -> {
            MR.strings.places_create_location_hint_searchTap
                .localizedString()
                .annotatedBold()
        }

        CreatePlaceAddressSearchUiMode.FULLY_EXPANDED -> {
            MR.strings.places_create_location_hint_searchOnly
                .localizedString()
                .annotatedBold()
        }
    }

    val currentStep: Int = radiusSteps.indexOfFirst { radiusMeters <= it }.takeIf { it >= 0 } ?: 0

    companion object {

        const val defaultRadiusMeters = 200
        val radiusSteps = arrayOf(
            50, 80, 100, 150, 200, 300, 400, 500, 600, 700, 800, 900, 1000,
            1200, 1500, 1800, 2000, 2200, 2500, 2800, 3000, 3500, 4000, 4500,
            5000, 6000, 7000, 8000, 9000, 10000, 12500, 15000, 17500, 20000
        )

        fun radiusForIndex(index: Int): Int {
            return radiusSteps.getOrNull(index) ?: defaultRadiusMeters
        }

        fun empty(
            groupId: GroupOrSpaceId,
            lat: Double? = null,
            lng: Double? = null,
            radiusMeters: Int,
            radiusText: String,
            nameSuggestions: List<SuggestionSectionChipData>
        ): CreatePlaceScreenState {
            return CreatePlaceScreenState(
                groupId = groupId,
                isNewPlace = true,
                screenStep = CreatePlaceScreenStep.NAME_SUGGESTION,
                name = "",
                image = ImageModel.emoji(Emojis.circles.random()),
                nameSuggestions = nameSuggestions,
                address = "",
                addressUiMode = if (lat == null || lng == null) {
                    CreatePlaceAddressSearchUiMode.SEARCH_EXPANDED
                } else {
                    CreatePlaceAddressSearchUiMode.HIDDEN
                },
                addressSuggestions = emptyList(),
                isAddressLoading = false,
                private = false,
                lat = lat,
                lng = lng,
                radiusMeters = radiusMeters,
                radiusText = radiusText,
                hasNameError = false,
                actionLoading = false,
                fullLoading = false,
                alertDialog = null
            )
        }
    }
}

enum class CreatePlaceScreenStep {
    NAME_SUGGESTION, FULL
}

enum class CreatePlaceAddressSearchUiMode {
    HIDDEN, SEARCH_EXPANDED, FULLY_EXPANDED
}

data class PlaceAddressSuggestion(
    val title: String,
    val info: String,
    val latLng: LatLng
)

sealed class CreatePlaceScreenEvent() {
    data object MissingInfo : CreatePlaceScreenEvent()
    data object RequestKeyboardFocusOnAddressInput : CreatePlaceScreenEvent()
    data object MissingLocation : CreatePlaceScreenEvent()
    data object PlaceUpdated : CreatePlaceScreenEvent()
}

sealed interface CreatePlaceScreenUiEvent {
    data object CreatePlace : CreatePlaceScreenUiEvent
    data class OnNameChanged(val name: String) : CreatePlaceScreenUiEvent
    data class OnRadiusChanged(val radiusMeters: Int) : CreatePlaceScreenUiEvent
    data class OnImageChanged(val image: ImageModel) : CreatePlaceScreenUiEvent
    data class OnAddressChanged(val address: String) : CreatePlaceScreenUiEvent
    data class OnNameSuggestionSelected(val suggestion: SuggestionSectionChipData) : CreatePlaceScreenUiEvent
    data class OnAddressSuggestionSelected(val suggestion: PlaceAddressSuggestion) : CreatePlaceScreenUiEvent

    data class ChangeSearchFieldFocus(val isFocused: Boolean) : CreatePlaceScreenUiEvent
    data object ToggleSearchFieldFocus : CreatePlaceScreenUiEvent
    data class OnMapTap(val lat: Double, val lng: Double) : CreatePlaceScreenUiEvent
    data class DialogAction(val action: AlertDialogAction) : CreatePlaceScreenUiEvent
}