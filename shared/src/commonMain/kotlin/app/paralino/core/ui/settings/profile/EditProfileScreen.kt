package app.paralino.core.ui.settings.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.presentation.settings.editprofile.EditProfileScreenEvent
import app.paralino.core.presentation.settings.editprofile.EditProfileScreenState
import app.paralino.core.presentation.settings.editprofile.EditProfileViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.ActionScaffold
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.BottomSheetPickerType
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.components.MainInputField
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.utils.Emojis
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest

@Composable
fun EditProfileScreen(
    appState: CoreUiState,
    vm: EditProfileViewModel,
    onBottomSheetAction: (BottomSheetPickerType.EmojiPickerBottomSheet) -> Unit,
    onExit: () -> Unit
) {

    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState(initial = EditProfileScreenState())

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                EditProfileScreenEvent.ProfileUpdated -> {
                    onExit()
                }
                null -> {
                    /* do nothing */
                }
            }
        }
    }

    EditProfileScreen(
        state = uiState,
        onImageClick = {
            onBottomSheetAction(BottomSheetPickerType.EmojiPickerBottomSheet(Emojis.userEmojis))
        },
        onNameChange = { vm.onNameChange(it) },
        onConfirm = { vm.proceed() }
    )
}

@Composable
fun EditProfileScreen(
    state: EditProfileScreenState,
    onImageClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    ActionScaffold(
        modifier = Modifier.fillMaxSize(),
        buttonState = AppButtonState.loadingStateWithCheck(state.actionLoading),
        buttonText = stringResource(MR.strings.button_save),
        buttonIcon = null,
        onConfirm = {
            focusManager.clearFocus()
            onConfirm()
        }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                }
                .verticalScroll(rememberScrollState())
                .padding(it)
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                ImageModelImage(
                    imageModel = state.image,
                    modifier = Modifier
                        .size(100.dp)
                        .clickable(onClick = onImageClick)
                )
            }

            Spacer(modifier = Modifier.size(20.dp))

            MainInputField(
                text = state.name,
                hint = stringResource(resource = MR.strings.settings_profile_name_placeholder),
                hasError = state.hasNameError,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
                capitalization = KeyboardCapitalization.Words,
                focusManager = focusManager,
                onInputChange = onNameChange
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}