package app.paralino.core.presentation.groups.settings

import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.billing.FeatureTableItemState
import app.paralino.core.presentation.billing.FeatureTableItemValue
import app.paralino.core.presentation.billing.FeatureTableState
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.common.MainAlertDialog

data class GroupSettingsScreenState(
    val id: String,
    val canEdit: Boolean,
    val hasDevFeatures: Boolean,
    val isMemberVerificationEnabled: Boolean,
    val isMemberVerificationOn: Boolean,
    val loadingAction: Boolean,
    val name: String,
    val image: ImageModel,
    val planName: String,
    val featureTableState: FeatureTableState,
    val alertDialog: MainAlertDialog?,
    val memberId: String?,
    val locationId: String?,
    val createdAtDateFormatted: String?,
    val memberSinceDateFormatted: String?,
)

sealed interface GroupSettingsScreenEvent {
    data object GroupDeleted : GroupSettingsScreenEvent
}

sealed interface GroupSettingsUiEvent {
    data object DeleteGroup : GroupSettingsUiEvent
    data object BlockGroup : GroupSettingsUiEvent
    data object LeaveGroup : GroupSettingsUiEvent
    data object ToggleMemberVerification : GroupSettingsUiEvent
    data class DialogAction(val action: AlertDialogAction) : GroupSettingsUiEvent
    data class OnCellItemClick(val table: FeatureTableItemState, val cellItem: FeatureTableItemValue) : GroupSettingsUiEvent
    data class Copy(val content: String) : GroupSettingsUiEvent
}