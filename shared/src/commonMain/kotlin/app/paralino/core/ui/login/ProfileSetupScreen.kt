package app.paralino.core.ui.login

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.paralino.core.MR
import app.paralino.core.presentation.settings.editprofile.EditProfileScreenState
import app.paralino.core.presentation.settings.editprofile.EditProfileViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.BottomSheetPickerType
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.dialogs.DefaultToolbar
import app.paralino.core.ui.settings.profile.EditProfileScreen
import app.paralino.core.utils.Emojis
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun ProfileSetupScreenCore(
    appState: CoreUiState,
    vm: EditProfileViewModel,
    onBottomSheetAction: (BottomSheetPickerType.EmojiPickerBottomSheet) -> Unit,
) {

    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState(initial = EditProfileScreenState())

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            DefaultToolbar(
                title = stringResource(MR.strings.welcome_account_edit_profile),
                navigationIcon = null
            )
        },
    ) {
        EditProfileScreen(
            state = uiState,
            onImageClick = { onBottomSheetAction(BottomSheetPickerType.EmojiPickerBottomSheet(Emojis.userEmojis)) },
            onNameChange = { vm.onNameChange(it) },
            onCreateGroupClick = { vm.proceed() }
        )
    }
}