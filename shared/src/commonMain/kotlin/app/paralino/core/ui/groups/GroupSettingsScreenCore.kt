package app.paralino.core.ui.groups

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.Resource
import app.paralino.core.models.data
import app.paralino.core.presentation.groups.settings.GroupSettingsScreenEvent
import app.paralino.core.presentation.groups.settings.GroupSettingsUiEvent
import app.paralino.core.presentation.groups.settings.GroupSettingsViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.LoadingDialog
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.settings.SettingsActionItem
import app.paralino.core.ui.settings.billing.FeatureTable
import app.paralino.core.ui.theme.AppColors
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest

@Composable
fun GroupSettingsScreenCore(
    appState: CoreUiState,
    vm: GroupSettingsViewModel,
    openBatteryNotificationsSettings: () -> Unit,
    openEditGroup: () -> Unit,
    onGroupDeleted: () -> Unit,
    openRawDataScreen: () -> Unit,
    openLocationHistorySettings: () -> Unit
) {
    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState(initial = Resource.NotInitialized)

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                GroupSettingsScreenEvent.GroupDeleted -> onGroupDeleted()
                null -> {}
            }
        }
    }

    val dialog = uiState.data?.alertDialog
    dialog?.Show {
        vm.onEvent(GroupSettingsUiEvent.DialogAction(AlertDialogAction(dialog, it)))
    }

    when (val state = uiState) {
        Resource.NotInitialized -> {}

        is Resource.Ready -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(state = rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 60.dp)
            ) {
                SettingsActionItem(
                    name = stringResource(MR.strings.raw_data_screen_title),
                    icon = CoreAppIcons.Security.asPainterResource()
                ) {
                    openRawDataScreen()
                }
                SettingsActionItem(
                    name = stringResource(MR.strings.group_settings_battery_notifications_title),
                    icon = CoreAppIcons.NotificationsActive.asPainterResource()
                ) {
                    openBatteryNotificationsSettings()
                }
                if (state.data.canEdit) {
                    SettingsActionItem(
                        name = stringResource(MR.strings.group_edit_screen_title),
                        icon = CoreAppIcons.Edit.asPainterResource()
                    ) {
                        openEditGroup()
                    }

//                    SettingsActionItem(
//                        modifier = Modifier.alpha(if (state.data.isMemberVerificationEnabled) 1f else 0.5f),
//                        name = stringResource(MR.strings.group_settings_verification_title),
//                        description = if (state.data.isMemberVerificationOn) {
//                            stringResource(MR.strings.group_settings_verification_description_on)
//                        } else {
//                            stringResource(MR.strings.group_settings_verification_description_off)
//                        },
//                        icon = CoreAppIcons.Security.asPainterResource(),
//                        type = SettingsActionItemType.Switch(checked = state.data.isMemberVerificationOn)
//                    ) {
//                        vm.onEvent(GroupSettingsUiEvent.ToggleMemberVerification)
//                    }

                    SettingsActionItem(
                        name = stringResource(MR.strings.history_delete_screen_title),
                        icon = CoreAppIcons.TimelinePathWithPins.asPainterResource(),
                        iconTint = AppColors.errorBackground
                    ) {
                        openLocationHistorySettings()
                    }

                    SettingsActionItem(
                        name = stringResource(MR.strings.group_details_menu_delete),
                        icon = CoreAppIcons.Delete.asPainterResource(),
                        iconTint = AppColors.errorBackground
                    ) {
                        vm.onEvent(GroupSettingsUiEvent.DeleteGroup)
                    }
                } else {
                    SettingsActionItem(
                        name = stringResource(MR.strings.history_delete_screen_title),
                        icon = CoreAppIcons.TimelinePathWithPins.asPainterResource(),
                        iconTint = AppColors.errorBackground
                    ) {
                        openLocationHistorySettings()
                    }
                    SettingsActionItem(
                        name = stringResource(MR.strings.group_details_menu_block),
                        icon = CoreAppIcons.Block.asPainterResource(),
                        iconTint = AppColors.errorBackground
                    ) {
                        vm.onEvent(GroupSettingsUiEvent.BlockGroup)
                    }
                    SettingsActionItem(
                        name = stringResource(MR.strings.group_details_menu_leave),
                        icon = CoreAppIcons.Logout.asPainterResource(),
                        iconTint = AppColors.errorBackground
                    ) {
                        vm.onEvent(GroupSettingsUiEvent.LeaveGroup)
                    }
                }

                if (state.data.canEdit) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 4.dp, top = 40.dp),
                        text = stringResource(MR.strings.paywall_current_plan_label, state.data.planName),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold)
                    )

                    FeatureTable(
                        modifier = Modifier,
                        table = state.data.featureTableState,
                        canExpand = false,
                        isExpanded = true,
                        onExpandPlans = {}
                    )
                }
            }

            if (state.data.loadingAction) {
                LoadingDialog(
                    text = stringResource(MR.strings.login_step_generic_loading),
                    progress = null
                )
            }
        }
    }
}