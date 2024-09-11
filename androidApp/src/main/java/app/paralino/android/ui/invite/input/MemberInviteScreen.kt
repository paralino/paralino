package app.paralino.android.ui.invite.input

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.MR
import app.paralino.core.presentation.invite.input.MemberInviteViewModel
import app.paralino.core.ui.components.dialogs.DefaultToolbar
import app.paralino.core.ui.components.dialogs.ToolbarButtons
import app.paralino.core.ui.groups.GroupBottomSheetPicker
import app.paralino.core.ui.groups.GroupPickerAction
import app.paralino.core.ui.groups.rememberGroupBottomSheetPickerState
import app.paralino.core.ui.invite.MemberInviteScreenCore
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MemberInviteScreen(
    appState: MainAppState,
    groupId: String?,
    code: String?,
    vm: MemberInviteViewModel = getViewModel { parametersOf(groupId, code) },
) {
    val pickerState = rememberGroupBottomSheetPickerState()
    Scaffold(
        topBar = {
            DefaultToolbar(
                title = stringResource(resource = MR.strings.invite_connect_screen_title),
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        }
    ) {
        MemberInviteScreenCore(
            appState = appState,
            vm = vm,
            contentPadding = it,
            onPickGroup = {
                appState.uiScope.launch { pickerState.show(it) }
            },
            onNavigateBack = {
                appState.navController.popBackStack()
            }
        )
    }

    GroupBottomSheetPicker(
        state = pickerState,
        onClick = {
            when (it) {
                is GroupPickerAction.GroupPicked -> {
                    vm.selectGroup(it.group)
                }

                GroupPickerAction.Close -> {
                    appState.uiScope.launch { pickerState.hide() }
                }

                else -> {}
            }
        }
    )
}
