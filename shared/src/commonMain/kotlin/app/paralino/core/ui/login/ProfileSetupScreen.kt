package app.paralino.core.ui.login

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.presentation.settings.editprofile.EditProfileScreenState
import app.paralino.core.presentation.settings.editprofile.EditProfileViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.BottomSheetsScreen
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.SettingsBigHeader
import app.paralino.core.ui.settings.profile.EditProfileScreen
import app.paralino.core.utils.Emojis
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun ProfileSetupScreenCore(
    appState: CoreUiState,
    vm: EditProfileViewModel,
    onBottomSheetAction: (BottomSheetsScreen) -> Unit,
) {

    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState(initial = EditProfileScreenState())

    Column() {
        SettingsBigHeader(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp),
            name = stringResource(MR.strings.welcome_account_edit_profile)
        )
        EditProfileScreen(
            state = uiState,
            onImageClick = { onBottomSheetAction(BottomSheetsScreen.EmojiPicker(Emojis.userEmojis)) },
            onNameChange = { vm.onNameChange(it) },
            onCreateGroupClick = { vm.proceed() }
        )
    }
}