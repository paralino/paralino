package app.paralino.android.ui.settings.profile

import android.annotation.SuppressLint
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.MR
import app.paralino.core.presentation.settings.editprofile.EditProfileViewModel
import app.paralino.core.ui.components.EmojiBottomSheetPicker
import app.paralino.core.ui.components.dialogs.DefaultToolbar
import app.paralino.core.ui.components.dialogs.ToolbarButtons
import app.paralino.core.ui.components.rememberEmojiBottomSheetPickerState
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalMaterialApi::class)
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun EditProfileScreen(
    appState: MainAppState,
    vm: EditProfileViewModel = getViewModel()
) {
    val pickerState = rememberEmojiBottomSheetPickerState()
    Scaffold(
        topBar = {
            DefaultToolbar(
                title = stringResource(resource = MR.strings.settings_account_edit_profile),
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        }
    ) {
        app.paralino.core.ui.settings.profile.EditProfileScreen(
            appState = appState,
            vm = vm,
            onBottomSheetAction = {
                appState.uiScope.launch {
                    pickerState.show(it.emojis)
                }
            },
            onExit = { appState.navController.popBackStack() }
        )
    }

    EmojiBottomSheetPicker(
        state = pickerState,
        onClick = {
            vm.onImageChange(it)
            appState.uiScope.launch { pickerState.hide() }
        }
    )
}