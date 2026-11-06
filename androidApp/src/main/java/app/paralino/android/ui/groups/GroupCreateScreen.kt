package app.paralino.android.ui.groups

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.presentation.groups.edit.CreateGroupViewModel
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.EmojiBottomSheetPicker
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.components.rememberEmojiBottomSheetPickerState
import app.paralino.core.ui.groups.GroupCreateScreenCore
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun GroupCreateScreen(
    appState: MainAppState,
    groupId: String?,
    vm: CreateGroupViewModel = koinViewModel { parametersOf(groupId) },
) {

    val pickerState = rememberEmojiBottomSheetPickerState()

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
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
                        screen = AppScreen.MainHome.GroupMemberAdd(
                            groupId = newGroupId,
                            code = null,
                            isOnboarding = true
                        ),
                        popUpTo = AppScreen.MainHome.MainMap::class
                    )
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
