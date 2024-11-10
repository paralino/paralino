package app.paralino.android.ui.invite.input

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.MR
import app.paralino.core.presentation.invite.input.MemberInviteViewModel
import app.paralino.core.ui.components.SmallActionButton
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
                },
                actions = {
                    if (isOnboarding) {
                        SmallActionButton(
                            modifier = Modifier.padding(end = 8.dp),
                            backgroundColor = MaterialTheme.colors.primary,
                            contentColor = MaterialTheme.colors.onPrimary,
                            icon = null,
                            hasEndArrow = true,
                            text = {
                                Text(
                                    text = stringResource(MR.strings.button_skip),
                                    fontSize = 14.sp
                                )
                            },
                            onClick = {
                                appState.navController.navigate(
                                    AppScreen.MainHome.PlacesList.route(
                                        groupId = groupId, isOnboarding = isOnboarding
                                    )
                                )
                            }
                        )
                    }
                }
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
            }
        )
    }

    BackHandler (enabled = isOnboarding) {
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
