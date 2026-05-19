package app.paralino.core.presentation.personal_space

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.presentation.groups.detail.LocationSharingState
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.presentation.groups.detail.GroupSpaceFocusedFeatureChildItem
import app.paralino.core.utils.ConditionErrorType

data class PersonalSpaceScreenState(
    val userName: String,
    val userImage: ImageModel?,
    val isLoadingData: Boolean,
    val loadingAction: Boolean,
    val sharingStatus: LocationSharingState?,
    val devicesWithSessions: List<UserDeviceOrSessionItemState>,
    val placesFeatureItems: List<GroupSpaceFocusedFeatureChildItem>,
    val devicesLimit: Int,
    val shouldShowPlanBanner: Boolean,
    val arePlacesOverLimit: Boolean?,
    val isSpaceOverLimit: Boolean?,
    val errors: List<ConditionErrorType>,
    val plan: PlanConfig,
    val alertDialog: MainAlertDialog?
) {
    val activeDevices: List<UserDeviceOrSessionItemState> = devicesWithSessions
        .filter { it.device?.location != null }

    val otherDevices: List<UserDeviceOrSessionItemState> = devicesWithSessions
        .filter { it.device?.location == null }

    val isCurrentDeviceActivated: Boolean = activeDevices.any {
        it.device?.isCurrentDevice == true
    }
}

sealed interface PersonalSpaceScreenEvent {
    data object OpenPaywall : PersonalSpaceScreenEvent
    data object OpenPlaces : PersonalSpaceScreenEvent
    data class OpenPlace(val item: GroupSpaceFocusedFeatureChildItem) : PersonalSpaceScreenEvent
    data object OpenSharingScreen : PersonalSpaceScreenEvent
    data object OpenTimeline : PersonalSpaceScreenEvent
    data object OpenMap : PersonalSpaceScreenEvent
}

sealed interface PersonalSpaceScreenUiEvent {
    data class DialogAction(val action: AlertDialogAction) : PersonalSpaceScreenUiEvent
    data object OpenPaywall : PersonalSpaceScreenUiEvent
    data object OpenPlaces : PersonalSpaceScreenUiEvent
    data class OpenPlace(val item: GroupSpaceFocusedFeatureChildItem) : PersonalSpaceScreenUiEvent
    data object OpenSharingScreen : PersonalSpaceScreenUiEvent
    data object OpenTimeline : PersonalSpaceScreenUiEvent
    data object OpenMap : PersonalSpaceScreenUiEvent
    data class OnDeviceEvent(val event: UserDeviceItemEvent) : PersonalSpaceScreenUiEvent
}
