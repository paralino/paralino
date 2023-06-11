package app.paralino.android.ui.groups.create

import android.annotation.SuppressLint
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.paralino.android.R
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.components.*
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.ui.shared.errorString
import app.paralino.android.ui.theme.AppIcons
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.android.utils.extensions.safeCast
import app.paralino.core.models.Payload
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.groups.CreateGroupScreenEvent
import app.paralino.core.presentation.groups.CreateGroupState
import app.paralino.core.presentation.groups.CreateGroupViewModel
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.MainInputField
import app.paralino.core.utils.Emojis
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun GroupCreateScreen(
    appState: MainAppState,
    groupId: String?,
    vm: CreateGroupViewModel = getViewModel { parametersOf(groupId) },
) {

    val state by vm.uiState.collectAsState(initial = Payload.Loading)

    val context = LocalContext.current
    LaunchedEffect(key1 = context) {
        vm.events.collectLatest { event ->
            when (event) {
                is CreateGroupScreenEvent.Error -> {
                    appState.showSnackbar(event.error.errorString(context))
                }
                is CreateGroupScreenEvent.GroupUpdated -> {
                    delay(400)
                    if (groupId.isNullOrBlank())
                        appState.navController.navigate(AppScreen.MainHome.GroupDetails.route(event.groupId)) {
                            this.popUpTo(route = AppScreen.MainHome.MainMap.routeSignature) {
                                this.inclusive = false
                            }
                        }
                    else appState.navController.popBackStack()
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
                    IconButton(onClick = { appState.navController.popBackStack() }) {
                        Icon(
                            painter = AppIcons.Close,
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
                DefaultEmptyState(content = stringResource(id = R.string.group_load_error))
            }
            is Payload.Success -> {
                GroupCreateScreen(
                    state = finalState.data,
                    nameLengthLimit = vm.groupNameLengthLimit,
                    onImageClick = {
                        appState.uiScope.launch {
                            appState.showBottomSheetForResult(BottomSheetsScreen.EmojiPicker(Emojis.groupEmojis))
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
            hint = stringResource(id = R.string.group_create_name_placeholder),
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
            icon = if (state.isNewGroup) null else AppIcons.Save,
            onClick = {
                focusManager.clearFocus()
                onCreateGroupClick(state.image, groupName)
            }
        ) {
            if (state.isNewGroup) {
                Text(text = stringResource(id = R.string.group_create_button_text))
            } else {
                Text(text = stringResource(id = R.string.button_save))
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