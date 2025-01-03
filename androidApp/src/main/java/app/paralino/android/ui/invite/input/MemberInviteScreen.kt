package app.paralino.android.ui.invite.input

import androidx.activity.compose.BackHandler
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import app.paralino.android.ui.groups.GroupBottomSheetPicker
import app.paralino.android.ui.groups.GroupPickerAction
import app.paralino.android.ui.groups.rememberGroupBottomSheetPickerState
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.MR
import app.paralino.core.presentation.invite.input.MemberInviteViewModel
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.invite.MemberInviteScreenCore
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MemberInviteScreen(
    appState: MainAppState,
    groupId: String?,
    code: String?,
    isOnboarding: Boolean,
    vm: MemberInviteViewModel = getViewModel { parametersOf(groupId, code) },
) {
    val pickerState = rememberGroupBottomSheetPickerState()
    Scaffold(
        topBar = {
            DefaultToolbar(
                title = stringResource(resource = MR.strings.invite_connect_screen_title),
                navigationIcon = if (isOnboarding) {
                    null
                } else {
                    { ToolbarButtons.Back { appState.navController.popBackStack() } }
                }
            )
        }
    ) {
        MemberInviteScreenCore(
            appState = appState,
            vm = vm,
            isOnboarding = isOnboarding,
            contentPadding = it,
            onPickGroup = {
                appState.uiScope.launch { pickerState.show(it) }
            },
            onMemberAdded = {
                if (isOnboarding) {
                    appState.navController.navigate(
                        AppScreen.MainHome.PlacesList.route(
                            groupId = groupId, isOnboarding = isOnboarding
                        )
                    )
                } else {
                    appState.navController.popBackStack()
                }
            },
            onSkipOnboarding = {
                appState.navController.navigate(
                    AppScreen.MainHome.PlacesList.route(
                        groupId = groupId, isOnboarding = isOnboarding
                    )
                )
            }
        )
    }

    BackHandler(enabled = isOnboarding) {
        // block back for onboarding
    }

    GroupBottomSheetPicker(
        state = pickerState,
        onClick = {
            when (it) {
                is GroupPickerAction.GroupPicked -> {
                    appState.uiScope.launch { pickerState.hide() }
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
