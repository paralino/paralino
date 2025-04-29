package app.paralino.core.presentation.settings.generic

import androidx.compose.ui.unit.Dp
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import org.jetbrains.compose.resources.DrawableResource

sealed interface GenericSettingsScreenState {
    abstract val screenTitle: String
    abstract val alertDialog: MainAlertDialog?

    data class Loading(
        override val screenTitle: String,
        override val alertDialog: MainAlertDialog? = null
    ): GenericSettingsScreenState

    data class EditScreen(
        override val screenTitle: String,
        override val alertDialog: MainAlertDialog? = null,
        val placeholder: String = "",
        val content: String = "",
        val hasContentError: Boolean = false,
        val actionLoading: Boolean? = false
    ): GenericSettingsScreenState

    data class ListItems(
        override val screenTitle: String,
        override val alertDialog: MainAlertDialog?,
        val items: List<GenericSettingListItem>
    ): GenericSettingsScreenState
}

sealed interface GenericSettingListItem {
    abstract val keyId: String

    data class StickyHeader(
        val content: String,
        override val keyId: String = content
    ): GenericSettingListItem

    data class Subtitle(
        val content: String,
        override val keyId: String = content
    ): GenericSettingListItem

    data class Spacer(
        val spacing: Dp,
        override val keyId: String
    ): GenericSettingListItem

    data class Notice(
        val content: String,
        val icon: DrawableResource,
        override val keyId: String = content
    ): GenericSettingListItem

    data class Switch<KEY>(
        val key: KEY,
        override val keyId: String,
        val title: String,
        val description: String,
        val icon: DrawableResource,
        val isChecked: Boolean,
        val isLoading: Boolean,
    ): GenericSettingListItem

    data class GroupOptions(
        val defaultId: String,
        override val keyId: String,
        val items: List<GroupOptionItem>
    ): GenericSettingListItem {
        val selectedItem: GroupOptionItem = items.firstOrNull { it.isSelected }
            ?: items.firstOrNull { it.id == defaultId }
            ?: items.first()
    }
}

data class GroupOptionItem(
    val key: Any?,
    val id: String,
    val title: String,
    val description: String?,
    val batteryImpact: String?,
    val highBatteryImpact: Boolean,
    val isPremiumFeatureEnabled: Boolean,
    val isSelected: Boolean
)

sealed interface GenericSettingsScreenEvent {
    data object OpenPaywall : GenericSettingsScreenEvent
}

sealed interface GenericSettingsScreenUiEvent {
    data class ToggleSwitch<KEY>(val switch: GenericSettingListItem.Switch<KEY>) : GenericSettingsScreenUiEvent
    data class SelectOptionItem(val option: GroupOptionItem) : GenericSettingsScreenUiEvent
    data class DialogAction(val action: AlertDialogAction) : GenericSettingsScreenUiEvent
}