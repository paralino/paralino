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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
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
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.groups.CreateGroupScreenEvent
import app.paralino.core.presentation.groups.CreateGroupState
import app.paralino.core.presentation.groups.CreateGroupViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.BottomSheetsScreen
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.components.MainInputField
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.errorString
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
fun GroupCreateScreenCore(
    appState: CoreUiState,
    vm: CreateGroupViewModel,
    onBottomSheetAction: (BottomSheetsScreen) -> Unit,
    groupUpdated: (String) -> Unit,
    onExit: () -> Unit,
) {

    val state by vm.uiState.collectAsState(initial = Payload.Loading)

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                is CreateGroupScreenEvent.Error -> {
                    appState.showSnackbar(event.error.errorString())
                }
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

    Scaffold(
        topBar = {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.background,
                elevation = 1.dp,
                title = { },
                navigationIcon = {
                    IconButton(onClick = { onExit() }) {
                        Icon(
                            painter = CoreAppIcons.Close.asPainterResource(),
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) {
        when (val finalState = state) {
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
                    nameLengthLimit = vm.groupNameLengthLimit,
                    onImageClick = {
                        onBottomSheetAction(BottomSheetsScreen.EmojiPicker(Emojis.groupEmojis))
                    },
                    onCreateGroupClick = { image, name ->
                        vm.createGroup(image, name)
                    }
                )
            }
        }
    }
}

@Composable
fun GroupCreateScreen(
    state: CreateGroupState,
    nameLengthLimit: Int,
    onImageClick: () -> Unit,
    onCreateGroupClick: (ImageModel, String) -> Unit,
) {

    var groupName by remember { mutableStateOf(state.name) }
    var rotation by remember { mutableStateOf(0f) }
    val focusManager = LocalFocusManager.current

    if (state.isNewGroup) {
        // Shake group image when creating group if user didn't use it to draw attention
        ShakeEffect {
            rotation = it
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            }
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.fillMaxHeight(1f))

        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            ImageModelImage(
                imageModel = state.image,
                modifier = Modifier
                    .size(100.dp)
                    .rotate(rotation)
                    .clickable {
                        onImageClick()
                    }
            )
        }

        Spacer(modifier = Modifier.size(20.dp))

        MainInputField(
            text = groupName,
            hint = stringResource(MR.strings.group_create_name_placeholder),
            hasError = state.hasError,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
            focusManager = focusManager,
            onInputChange = { groupName = it.take(nameLengthLimit) }
        )

        Spacer(modifier = Modifier.size(80.dp))

        FilledButton(
            style = ButtonStyle.BIG,
            state = AppButtonState.loadingStateWithCheck(state.isLoading),
            icon = if (state.isNewGroup) null else CoreAppIcons.Save.asPainterResource(),
            onClick = {
                focusManager.clearFocus()
                onCreateGroupClick(state.image, groupName)
            }
        ) {
            if (state.isNewGroup) {
                Text(text = stringResource(MR.strings.group_create_button_text))
            } else {
                Text(text = stringResource(MR.strings.button_save))
            }
        }

        Spacer(modifier = Modifier.fillMaxHeight(1f))
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