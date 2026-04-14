package app.paralino.core.presentation.settings.generic

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.paralino.core.models.location.LatLng
import app.paralino.core.models.settings.MapsSDKProvider
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.settings.SettingsActionItemType
import app.paralino.core.utils.ConditionErrorType
import org.jetbrains.compose.resources.DrawableResource

data class GenericSettingsScreenState(
    val screenTitle: String,
    val alertDialog: MainAlertDialog?,
    val content: GenericSettingsScreenStateContent
)

sealed interface GenericSettingsScreenStateContent {

    data object Loading: GenericSettingsScreenStateContent

    data class EditScreen(
        val placeholder: String = "",
        val content: String = "",
        val hasContentError: Boolean = false,
        val actionLoading: Boolean? = false
    ): GenericSettingsScreenStateContent

    data class EmptyState(
        val icon: DrawableResource,
        val content: String,
        val buttonText: String?,
        val buttonLoading: Boolean? = false
    ): GenericSettingsScreenStateContent

    data class ListItems(
        val isFullScreenLoading: Boolean = false,
        val loadingText: String? = null,
        val isUserScrollEnabled: Boolean = true,
        val items: List<GenericSettingListItem>
    ): GenericSettingsScreenStateContent
}

sealed interface GenericSettingListItem {
    abstract val keyId: String

    data class ConditionError(
        val type: ConditionErrorType,
        override val keyId: String = type::class.toString()
    ): GenericSettingListItem

    data class StickyHeader(
        val content: String,
        override val keyId: String = content
    ): GenericSettingListItem

    data class Subtitle(
        val content: String,
        val bottomPadding: Dp = 20.dp,
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

    data class EmptyState(
        val isVisible: Boolean,
        val icon: DrawableResource,
        val content: String,
        val buttonText: String?,
        val buttonLoading: Boolean? = false,
        override val keyId: String = content
    ): GenericSettingListItem

    data class Switch<KEY>(
        val key: KEY,
        override val keyId: String,
        val title: String,
        val description: String,
        val icon: DrawableResource?,
        val isChecked: Boolean,
        val isEnabled: Boolean,
        val isLoading: Boolean,
        val isVisible: Boolean
    ): GenericSettingListItem

    data class GroupOptions(
        val defaultId: String,
        override val keyId: String,
        val items: List<GroupOptionItem>
    ): GenericSettingListItem {
        val visibleItems = items.filter { it.isVisible }
        val selectedItem: GroupOptionItem = items.firstOrNull { it.isSelected }
            ?: items.firstOrNull { it.id == defaultId }
            ?: items.first()
    }

    data class SettingsActionItem(
        val text: String,
        val icon: DrawableResource,
        val type: SettingsActionItemType,
        val isVisible: Boolean,
        val tag: String?,
        val description: String?,
        override val keyId: String = text
    ): GenericSettingListItem


    data class Map(
        override val keyId: String,
        val mapSDKProvider: MapsSDKProvider,
        val height: Dp,
        val isDarkMode: Boolean?,
        val centerLocation: LatLng,
        val zoomLevel: Float,
        val isVisible: Boolean,
    ): GenericSettingListItem
}

data class GroupOptionItem(
    val key: Any?,
    val id: String,
    val title: String,
    val description: String?,
    val tag: String?,
    val isTagWarning: Boolean,
    val isPremiumFeatureEnabled: Boolean,
    val isSelected: Boolean,
    val isLoading: Boolean = false,
    val isVisible: Boolean = true
)

sealed interface GenericSettingsScreenEvent {
    data object OpenPaywall : GenericSettingsScreenEvent
    data object OpenNotificationSocketChannelSettings : GenericSettingsScreenEvent
    data object AskForNotificationPermission : GenericSettingsScreenEvent
    data object AskForBackgroundAccessPermission : GenericSettingsScreenEvent
}

sealed interface GenericSettingsScreenUiEvent {
    data class ToggleSwitch<KEY>(val switch: GenericSettingListItem.Switch<KEY>) : GenericSettingsScreenUiEvent
    data class ConditionErrorItemClick(val item: GenericSettingListItem.ConditionError) : GenericSettingsScreenUiEvent
    data class SelectOptionItem(val groupOptions: GenericSettingListItem.GroupOptions, val option: GroupOptionItem) : GenericSettingsScreenUiEvent
    data class SelectActionItem(val item: GenericSettingListItem.SettingsActionItem) : GenericSettingsScreenUiEvent
    data class DialogAction(val action: AlertDialogAction) : GenericSettingsScreenUiEvent
    data object GlobalEmptyStateActionClick : GenericSettingsScreenUiEvent
    data class OnMapInteraction(val isTouchingMap: Boolean) : GenericSettingsScreenUiEvent
    data class EmbeddedEmptyStateActionClick(val item: GenericSettingListItem.EmptyState) : GenericSettingsScreenUiEvent
}