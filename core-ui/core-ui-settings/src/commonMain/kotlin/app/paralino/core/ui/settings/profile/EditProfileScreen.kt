package app.paralino.core.ui.settings.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.presentation.common.BottomSheetPickerType
import app.paralino.core.presentation.settings.editprofile.EditProfileScreenEvent
import app.paralino.core.presentation.settings.editprofile.EditProfileScreenState
import app.paralino.core.presentation.settings.editprofile.EditProfileViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.ActionScaffold
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.groups.components.InitialGroupContentCreationComponent
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
        canShakeImage = false,
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
    canShakeImage: Boolean,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                }
                .padding(horizontal = 20.dp)
        ) {
            InitialGroupContentCreationComponent(
                modifier = Modifier.fillMaxSize().padding(it),
                focusManager = focusManager,
                shakeImage = canShakeImage,
                image = state.image,
                name = state.name,
                namePlaceholder = null,
                maxTextLength = EditProfileScreenState.profileNameLengthLimit,
                hint = stringResource(MR.strings.settings_profile_name_placeholder),
                hasNameError = state.hasNameError,
                suggestions = emptyList(),
                bottomContent = {},
                onImageClick = onImageClick,
                onNameChanged = onNameChange,
                onSuggestionSelected = {}
            )
        }
    }
}