package app.paralino.core.ui.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.common.BottomSheetPickerType
import app.paralino.core.presentation.groups.edit.CreateGroupScreenEvent
import app.paralino.core.presentation.groups.edit.CreateGroupScreenUiEvent
import app.paralino.core.presentation.groups.edit.CreateGroupState
import app.paralino.core.presentation.groups.edit.CreateGroupViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.ActionScaffold
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.groups.components.InitialGroupContentCreationComponent
import app.paralino.core.utils.Emojis
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
fun GroupCreateScreenCore(
    appState: CoreUiState,
    vm: CreateGroupViewModel,
    onBottomSheetAction: (BottomSheetPickerType.EmojiPickerBottomSheet) -> Unit,
    groupUpdated: (String) -> Unit
) {

    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState()

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                is CreateGroupScreenEvent.GroupUpdated -> {
                    delay(400)
                    groupUpdated(event.groupId)
                }

                null -> {
                    /* do nothing */
                }
            }
        }
    }

    val dialog = uiState.data?.alertDialog
    dialog?.Show {
        vm.onEvent(CreateGroupScreenUiEvent.DialogAction(AlertDialogAction(dialog, it)))
    }

    when (val finalState = uiState) {
        Payload.Loading -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        }

        is Payload.Error -> {
            DefaultEmptyState(content = stringResource(MR.strings.group_load_error))
        }

        is Payload.Success -> {
            GroupCreateScreen(
                state = finalState.data,
                onImageClick = {
                    onBottomSheetAction(BottomSheetPickerType.EmojiPickerBottomSheet(Emojis.groupEmojis))
                },
                onEvent = {
                    vm.onEvent(it)
                }
            )
        }
    }
}

@Composable
private fun GroupCreateScreen(
    state: CreateGroupState,
    onImageClick: () -> Unit,
    onEvent: (CreateGroupScreenUiEvent) -> Unit
) {
    val focusManager = LocalFocusManager.current
    ActionScaffold(
        modifier = Modifier.fillMaxSize(),
        buttonState = AppButtonState.loadingStateWithCheck(state.isLoading),
        buttonText = if (state.isNewGroup) {
            stringResource(MR.strings.group_create_button_text)
        } else {
            stringResource(MR.strings.button_save)
        },
        buttonIcon = null,
        onConfirm = {
            focusManager.clearFocus()
            onEvent(CreateGroupScreenUiEvent.CreateGroup)
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
                shakeImage = state.isNewGroup,
                image = state.image,
                name = state.name,
                hint = stringResource(MR.strings.group_create_name_placeholder),
                hasNameError = state.hasNameError,
                suggestions = state.suggestions,
                bottomContent = {},
                onImageClick = onImageClick,
                onNameChanged = { onEvent(CreateGroupScreenUiEvent.OnGroupNameChange(it)) },
                onSuggestionSelected = {
                    onEvent(CreateGroupScreenUiEvent.OnGroupSuggestionClicked(it))
                }
            )
        }
    }
}