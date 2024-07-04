package app.paralino.android.ui.login

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.presentation.settings.editprofile.EditProfileViewModel
import app.paralino.core.ui.components.EmojiBottomSheetPicker
import app.paralino.core.ui.components.rememberEmojiBottomSheetPickerState
import app.paralino.core.ui.login.ProfileSetupScreenCore
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ProfileSetupScreen(
    appState: MainAppState,
    vm: EditProfileViewModel = getViewModel(),
) {
    val pickerState = rememberEmojiBottomSheetPickerState()
    ProfileSetupScreenCore(
        appState = appState,
        vm = vm,
        onBottomSheetAction = {
            appState.uiScope.launch {
                pickerState.show(it.emojis)
            }
        }
    )

    EmojiBottomSheetPicker(
        state = pickerState,
        onClick = {
            vm.onImageChange(it)
            appState.uiScope.launch { pickerState.hide() }
        }
    )
}