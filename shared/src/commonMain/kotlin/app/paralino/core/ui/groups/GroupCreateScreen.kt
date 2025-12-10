package app.paralino.core.ui.groups

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.presentation.groups.edit.CreateGroupScreenEvent
import app.paralino.core.presentation.groups.edit.CreateGroupScreenUiEvent
import app.paralino.core.presentation.groups.edit.CreateGroupState
import app.paralino.core.presentation.groups.edit.CreateGroupViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.ActionScaffold
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.BottomSheetPickerType
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.components.MainInputField
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.groups.components.ChipsSuggestionsSection
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

    var rotation by remember { mutableStateOf(0f) }
    val focusManager = LocalFocusManager.current

    if (state.isNewGroup) {
        // Shake group image when creating group if user didn't use it to draw attention
        ShakeEffect {
            rotation = it
        }
    }

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
                        .rotate(rotation)
                        .clickable(onClick = onImageClick)
                )
            }

            Spacer(modifier = Modifier.size(20.dp))

            MainInputField(
                modifier = Modifier.height(70.dp),
                text = state.name,
                hint = stringResource(MR.strings.group_create_name_placeholder),
                hasError = state.hasNameError,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
                focusManager = focusManager,
                onInputChange = {
                    onEvent(CreateGroupScreenUiEvent.OnGroupNameChange(it))
                }
            )

            if (state.suggestions.isNotEmpty()) {
                ChipsSuggestionsSection(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    suggestions = state.suggestions,
                    onSuggestionClick = {
                        onEvent(CreateGroupScreenUiEvent.OnGroupSuggestionClicked(it))
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun ShakeEffect(
    abort: Boolean = false,
    onValueChange: (Float) -> Unit,
) {
    val shakeAnimation = remember { Animatable(0f) }
    LaunchedEffect(onValueChange) {
        delay(2000)

        if (!abort) {
            val shakeAmount = 8f
            shakeAnimation.animateTo(
                -shakeAmount,
                animationSpec = tween(30, 0, easing = LinearOutSlowInEasing)
            ) {
                onValueChange(this.value)
            }

            shakeAnimation.animateTo(
                shakeAmount, animationSpec = repeatable(
                    iterations = 5,
                    animation = tween(60, 0, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            ) {
                onValueChange(this.value)
            }

            shakeAnimation.animateTo(
                0f,
                animationSpec = tween(30, 0, easing = LinearOutSlowInEasing)
            ) {
                onValueChange(this.value)
            }
        }
    }
}