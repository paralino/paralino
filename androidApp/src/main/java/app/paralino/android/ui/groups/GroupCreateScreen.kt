package app.paralino.android.ui.groups

import android.annotation.SuppressLint
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.presentation.groups.CreateGroupViewModel
import app.paralino.core.ui.components.EmojiBottomSheetPicker
import app.paralino.core.ui.components.dialogs.DefaultToolbar
import app.paralino.core.ui.components.dialogs.ToolbarButtons
import app.paralino.core.ui.components.rememberEmojiBottomSheetPickerState
import app.paralino.core.ui.groups.GroupCreateScreenCore
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterialApi::class)
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun GroupCreateScreen(
    appState: MainAppState,
    groupId: String?,
    vm: CreateGroupViewModel = getViewModel { parametersOf(groupId) },
) {

    val pickerState = rememberEmojiBottomSheetPickerState()

    Scaffold(
        topBar = {
            DefaultToolbar(
                title = null,
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        }
    ) {
        GroupCreateScreenCore(
            appState = appState,
            vm = vm,
            onBottomSheetAction = {
                appState.uiScope.launch {
                    pickerState.show(it.emojis)
                }
            },
            groupUpdated = { newGroupId ->
                if (vm.isGroupCreate()) {
                    appState.navController.navigate(
                        route = AppScreen.MainHome.GroupMemberAdd.route(
                            groupId = newGroupId,
                            code = null,
                            isOnboarding = true
                        )
                    ) {
                        this.popUpTo(route = AppScreen.MainHome.MainMap.routeSignature) {
                            this.inclusive = false
                        }
                    }
                } else {
                    appState.navController.popBackStack()
                }
            }
        )
    }

    EmojiBottomSheetPicker(
        state = pickerState,
        onClick = {
            vm.updateImage(it)
            appState.uiScope.launch { pickerState.hide() }
        }
    )
}
