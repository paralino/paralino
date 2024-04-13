package app.paralino.android.ui.invite.input

import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.MR
import app.paralino.core.presentation.groups.GroupItemState
import app.paralino.core.presentation.invite.input.MemberInviteViewModel
import app.paralino.core.ui.components.dialogs.DefaultToolbar
import app.paralino.core.ui.components.dialogs.ToolbarButtons
import app.paralino.core.ui.invite.MemberInviteScreenCore
import app.paralino.core.utils.extensions.safeCast
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MemberInviteScreen(
    appState: MainAppState,
    groupId: String?,
    code: String?,
    vm: MemberInviteViewModel = getViewModel { parametersOf(groupId, code) },
) {
    Scaffold(
        topBar = {
            DefaultToolbar(
                title = stringResource(resource = MR.strings.group_details_add_member_button),
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        }
    ) {
        MemberInviteScreenCore(
            appState = appState,
            vm = vm,
            contentPadding = it,
            onPickGroup = {
                appState.uiScope.launch {
                    appState.showBottomSheetForResult(it).safeCast<GroupItemState>()?.let {
                        vm.selectGroup(it)
                    }
                }
            },
            onNavigateBack = {
                appState.navController.popBackStack()
            }
        )
    }
}
