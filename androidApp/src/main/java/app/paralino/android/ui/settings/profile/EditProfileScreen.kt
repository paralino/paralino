package app.paralino.android.ui.settings.profile

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.resources.moko.*
import app.paralino.core.presentation.settings.editprofile.EditProfileViewModel
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.EmojiBottomSheetPicker
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.components.rememberEmojiBottomSheetPickerState
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun EditProfileScreen(
    appState: MainAppState,
    vm: EditProfileViewModel = koinViewModel()
) {
    val pickerState = rememberEmojiBottomSheetPickerState()
    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
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