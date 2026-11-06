package app.paralino.core.ui.login

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.paralino.core.resources.moko.*
import app.paralino.core.presentation.settings.editprofile.EditProfileScreenState
import app.paralino.core.presentation.settings.editprofile.EditProfileViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.presentation.common.BottomSheetPickerType
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.settings.profile.EditProfileScreen
import app.paralino.core.utils.Emojis
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun ProfileSetupScreenCore(
    modifier: Modifier = Modifier,
    appState: CoreUiState,
    vm: EditProfileViewModel,
    onBottomSheetAction: (BottomSheetPickerType.EmojiPickerBottomSheet) -> Unit,
) {

    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState(initial = EditProfileScreenState())

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DefaultToolbar(
                title = stringResource(MR.strings.welcome_account_edit_profile),
                navigationIcon = null
            )
        },
    ) {
        EditProfileScreen(
            state = uiState,
            canShakeImage = true,
            onImageClick = { onBottomSheetAction(BottomSheetPickerType.EmojiPickerBottomSheet(Emojis.userEmojis)) },
            onNameChange = { vm.onNameChange(it) },
            onConfirm = { vm.proceed() }
        )
    }
}