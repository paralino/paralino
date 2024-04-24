package app.paralino.core.ui.groups

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.Resource
import app.paralino.core.models.data
import app.paralino.core.presentation.groups.GroupSettingsScreenEvent
import app.paralino.core.presentation.groups.GroupSettingsUiEvent
import app.paralino.core.presentation.groups.GroupSettingsViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.LoadingDialog
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.settings.SettingsActionItem
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest

@Composable
fun GroupSettingsScreenCore(
    appState: CoreUiState,
    vm: GroupSettingsViewModel,
    openEditGroup: () -> Unit,
    onGroupDeleted: () -> Unit,
    openRawDataScreen: () -> Unit,
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
                        name = stringResource(MR.strings.group_details_menu_delete),
                        icon = CoreAppIcons.Delete.asPainterResource(),
                        iconTint = MaterialTheme.colors.error
                    ) {
                        vm.onEvent(GroupSettingsUiEvent.DeleteGroup)
                    }
                } else {
                    SettingsActionItem(
                        name = stringResource(MR.strings.group_details_menu_block),
                        icon = CoreAppIcons.Block.asPainterResource(),
                        iconTint = MaterialTheme.colors.error
                    ) {
                        vm.onEvent(GroupSettingsUiEvent.BlockGroup)
                    }
                    SettingsActionItem(
                        name = stringResource(MR.strings.group_details_menu_leave),
                        icon = CoreAppIcons.Logout.asPainterResource(),
                        iconTint = MaterialTheme.colors.error
                    ) {
                        vm.onEvent(GroupSettingsUiEvent.LeaveGroup)
                    }
                }
            }

            if (state.data.loadingAction) {
                LoadingDialog(text = stringResource(MR.strings.login_step_generic_loading), progress = null)
            }
        }
    }
}