package com.zbapp.locationtrack.ui.groups.create

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Save
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zbapp.core.models.Payload
import com.zbapp.core.models.base.ImageModel
import com.zbapp.core.presentation.groups.CreateGroupScreenEvent
import com.zbapp.core.presentation.groups.CreateGroupState
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.MainAppState
import com.zbapp.locationtrack.ui.components.*
import com.zbapp.locationtrack.ui.shared.errorString
import com.zbapp.locationtrack.ui.theme.ButtonStyle
import com.zbapp.locationtrack.utils.extensions.safeCast
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GroupCreateScreen(
    appState: MainAppState,
    groupId: String?,
    vm: CreateGroupViewModel = getViewModel { parametersOf(groupId) },
) {

    val state by vm.uiState.collectAsState(initial = Payload.Loading)

    val context = LocalContext.current
    LaunchedEffect(key1 = context) {
        vm.events.collect { event ->
            when (event) {
                is CreateGroupScreenEvent.Error -> {
                    appState.showSnackbar(event.error.errorString(context))
                }
                CreateGroupScreenEvent.GroupUpdated -> {
                    delay(400)
                    appState.navController.popBackStack()
                }
                null -> {
                    /* do nothing */
                }
            }
        }
    }

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
            DefaultEmptyState(content = stringResource(id = R.string.group_load_error))
        }
        is Payload.Success -> {
            GroupCreateScreen(
                state = finalState.data,
                nameLengthLimit = vm.groupNameLengthLimit,
                onImageClick = {
                    appState.uiScope.launch {
                        appState.showBottomSheetForResult(BottomSheetsScreen.EmojiPicker)
                            .safeCast<ImageModel>()?.let {
                                vm.updateImage(it)
                            }
                    }
                },
                onCreateGroupClick = { image, name ->
                    vm.createGroup(image, name)
                }
            )
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
            .padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.size(100.dp))

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

        Spacer(modifier = Modifier.size(40.dp))

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            value = groupName,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions {
                focusManager.clearFocus()
            },
            placeholder = {
                Text(
                    stringResource(id = R.string.group_create_name_placeholder),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            onValueChange = { groupName = it.take(nameLengthLimit) },
            shape = MaterialTheme.shapes.medium,
            isError = state.hasError,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                backgroundColor = MaterialTheme.colors.surface,
                unfocusedBorderColor = MaterialTheme.colors.surface,
            )
        )

        Spacer(modifier = Modifier.size(100.dp))

        FilledButton(
            style = ButtonStyle.BIG,
            state = AppButtonState.loadingStateWithCheck(state.isLoading),
            icon = if (state.isNewGroup) Icons.Rounded.GroupAdd else Icons.Rounded.Save,
            onClick = { onCreateGroupClick(state.image, groupName) }
        ) {
            if (state.isNewGroup) {
                Text(text = stringResource(id = R.string.group_create_button_text))
            } else {
                Text(text = stringResource(id = R.string.button_save))
            }
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