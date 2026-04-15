package app.paralino.core.ui.places

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.presentation.places.create.CreatePlaceAddressSearchUiMode
import app.paralino.core.presentation.places.create.CreatePlaceScreenEvent
import app.paralino.core.presentation.places.create.CreatePlaceScreenState
import app.paralino.core.presentation.places.create.CreatePlaceScreenStep
import app.paralino.core.presentation.places.create.CreatePlaceScreenUiEvent
import app.paralino.core.presentation.places.create.CreatePlaceViewModel
import app.paralino.core.presentation.places.create.PlaceAddressSuggestion
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.BottomSheetPickerType
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.EditImageIcon
import app.paralino.core.ui.components.EmbeddedSlider
import app.paralino.core.ui.components.EmbeddedSliderSize
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.components.MainInputBasicField
import app.paralino.core.ui.components.MainInputField
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.ScaffoldBottomBarButton
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.ui.components.dialogs.LoadingDialog
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.groups.ShakeEffect
import app.paralino.core.ui.groups.components.ChipsSuggestionsSection
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.LocalScreenDimensionType
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.conditional
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun CreatePlaceScreenCore(
    appState: CoreUiState,
    vm: CreatePlaceViewModel,
    onNavigateBack: () -> Unit,
    onBottomSheetAction: (BottomSheetPickerType.EmojiPickerBottomSheet) -> Unit,
    mapContent: @Composable (CreatePlaceScreenState) -> Unit,
) {

    MessageDispatcherEffect(vm, appState)
    val screenDimensionType = LocalScreenDimensionType.current
    LaunchedEffect(key1 = screenDimensionType.isReady) {
        vm.onEvent(CreatePlaceScreenUiEvent.OnUiReady(size = screenDimensionType))
    }

    val uiState by vm.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val inputMethod = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                is CreatePlaceScreenEvent.MissingLocation -> {
                    vm.showSnackbar(MR.strings.places_create_error_no_place.localizedString())
                }

                is CreatePlaceScreenEvent.MissingInfo -> {
                    vm.showSnackbar(MR.strings.form_generic_validation.localizedString())
                }

                is CreatePlaceScreenEvent.PlaceUpdated -> {
                    delay(400)
                    onNavigateBack()
                }

                is CreatePlaceScreenEvent.RequestKeyboardFocusOnAddressInput -> {
                    scope.launch {
                        delay(100)
                        inputMethod?.show()
                        try {
                            focusRequester.requestFocus()
                        } catch (e: Exception) {
                        }
                    }
                }

                null -> {
                    /* do nothing */
                }
            }
        }
    }

    val dialog = uiState.data?.alertDialog
    dialog?.Show {
        vm.onEvent(CreatePlaceScreenUiEvent.DialogAction(AlertDialogAction(dialog, it)))
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            when (val finalState = uiState) {
                is Payload.Error -> {}
                Payload.Loading -> {}
                is Payload.Success -> {
                    AnimatedVisibility (
                        !screenDimensionType.isSmall ||
                        finalState.data.screenStep == CreatePlaceScreenStep.NAME_SUGGESTION ||
                        finalState.data.addressUiMode == CreatePlaceAddressSearchUiMode.HIDDEN
                    ) {
                        ScaffoldBottomBarButton(
                            buttonState = AppButtonState.loadingStateWithCheck(finalState.data.actionLoading),
                            buttonText = when (finalState.data.screenStep) {
                                CreatePlaceScreenStep.NAME_SUGGESTION -> {
                                    stringResource(MR.strings.button_next)
                                }

                                CreatePlaceScreenStep.FULL -> {
                                    if (finalState.data.isNewPlace) {
                                        stringResource(MR.strings.places_create_button_create)
                                    } else {
                                        stringResource(MR.strings.button_save)
                                    }
                                }
                            },
                            buttonIcon = null,
                            onClick = { vm.onEvent(CreatePlaceScreenUiEvent.CreatePlace) }
                        )
                    }
                }
            }
        }
    ) {
        when (val finalState = uiState) {
            Payload.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }

            is Payload.Error -> {
                DefaultEmptyState(content = stringResource(MR.strings.error_load_data_generic))
            }

            is Payload.Success -> {
                CreatePlaceScreenCore(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it),
                    focusRequester = focusRequester,
                    state = finalState.data,
                    onImageClick = {
                        onBottomSheetAction(BottomSheetPickerType.EmojiPickerBottomSheet(Emojis.groupEmojis))
                    },
                    onEvent = vm::onEvent,
                    mapContent = mapContent
                )

                if (finalState.data.fullLoading) {
                    LoadingDialog(
                        text = stringResource(MR.strings.login_step_generic_loading),
                        progress = null
                    )
                }
            }
        }
    }
}

@Composable
private fun CreatePlaceScreenCore(
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester,
    state: CreatePlaceScreenState,
    onImageClick: () -> Unit,
    onEvent: (CreatePlaceScreenUiEvent) -> Unit,
    mapContent: @Composable (CreatePlaceScreenState) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            }
            .padding(horizontal = 20.dp)
    ) {
        AnimatedVisibility(
            visible = state.screenStep == CreatePlaceScreenStep.NAME_SUGGESTION,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            CreatePlaceNameSuggestionStep(
                modifier = Modifier.fillMaxSize(),
                focusManager = focusManager,
                state = state,
                onImageClick = onImageClick,
                onEvent = onEvent
            )
        }

        AnimatedVisibility(
            visible = state.screenStep == CreatePlaceScreenStep.FULL,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            CreatePlaceMapStep(
                modifier = Modifier.fillMaxSize(),
                focusManager = focusManager,
                focusRequester = focusRequester,
                state = state,
                onImageClick = onImageClick,
                onEvent = onEvent,
                mapContent = mapContent
            )
        }
    }
}

@Composable
private fun CreatePlaceNameSuggestionStep(
    modifier: Modifier = Modifier,
    focusManager: FocusManager,
    state: CreatePlaceScreenState,
    onImageClick: () -> Unit,
    onEvent: (CreatePlaceScreenUiEvent) -> Unit,
) {
    var rotation by remember { mutableStateOf(0f) }
    if (state.isNewPlace) {
        // Shake the image if user didn't use it to draw attention
        ShakeEffect {
            rotation = it
        }
    }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        val isSmallScreen = LocalScreenDimensionType.current.isSmall

        Box(
            modifier = Modifier.size(if (isSmallScreen) 80.dp else 120.dp),
            contentAlignment = Alignment.Center
        ) {
            ImageModelImage(
                imageModel = state.image,
                modifier = Modifier
                    .size(if (isSmallScreen) 70.dp else 100.dp)
                    .rotate(rotation)
                    .clickable(onClick = onImageClick)
            )
            EditImageIcon(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clickable(onClick = onImageClick)
            )
        }

        Spacer(modifier = Modifier.size(if (isSmallScreen) 10.dp else 20.dp))

        MainInputField(
            modifier = Modifier.height(70.dp),
            text = state.name,
            hint = stringResource(MR.strings.places_create_name_placeholder),
            hasError = state.hasNameError,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
            focusManager = focusManager,
            onInputChange = {
                onEvent(CreatePlaceScreenUiEvent.OnNameChanged(it))
            }
        )

        if (state.nameSuggestions.isNotEmpty()) {
            ChipsSuggestionsSection(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                suggestions = state.nameSuggestions,
                onSuggestionClick = {
                    onEvent(CreatePlaceScreenUiEvent.OnNameSuggestionSelected(it))
                    scope.launch {
                        scrollState.animateScrollTo(0)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CreatePlaceMapStep(
    modifier: Modifier = Modifier,
    focusManager: FocusManager,
    focusRequester: FocusRequester,
    state: CreatePlaceScreenState,
    onImageClick: () -> Unit,
    onEvent: (CreatePlaceScreenUiEvent) -> Unit,
    mapContent: @Composable (CreatePlaceScreenState) -> Unit,
) {
    val isSmallScreen = LocalScreenDimensionType.current.isSmall
    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        AnimatedVisibility(
            visible = state.addressUiMode == CreatePlaceAddressSearchUiMode.HIDDEN && !isSmallScreen
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(64.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                ImageModelImage(
                    imageModel = state.image,
                    modifier = Modifier
                        .size(56.dp)
                        .clickable { onImageClick() }
                        .align(Alignment.Bottom),
                )

                Spacer(modifier = Modifier.size(8.dp))

                MainInputField(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    text = state.name,
                    hint = stringResource(MR.strings.places_create_name_placeholder),
                    focusManager = focusManager,
                    hasError = state.hasNameError,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                    onInputChange = {
                        onEvent(CreatePlaceScreenUiEvent.OnNameChanged(it))
                    }
                )
            }
        }

        // map
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = if (isSmallScreen) 12.dp else 20.dp
                )
                .weight(1f)
                .background(
                    color = MaterialTheme.colors.background,
                    shape = RoundedCornerShape(20.dp)
                )
                .border(
                    border = BorderStroke(6.dp, MaterialTheme.colors.background),
                    shape = RoundedCornerShape(20.dp)
                )
                .dropShadow(
                    shape = RoundedCornerShape(20.dp),
                    shadow = Shadow(
                        radius = 12.dp,
                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.2f),
                    )
                )
                .clip(RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colors.background)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .padding(6.dp),
                    shape = RoundedCornerShape(15.dp),
                    color = MaterialTheme.colors.background,
                ) {
                    mapContent(state)
                }

                val sliderSize = remember {
                    EmbeddedSliderSize(minStepWidth = 120.dp)
                }
                AnimatedVisibility(visible = state.addressUiMode == CreatePlaceAddressSearchUiMode.HIDDEN) {
                    EmbeddedSlider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp)
                            .padding(bottom = 6.dp, top = 2.dp)
                            .height(sliderSize.minHeight),
                        title = stringResource(MR.strings.places_create_seekbar_info),
                        value = state.radiusText,
                        size = sliderSize,
                        progress = state.currentStep,
                        steps = CreatePlaceScreenState.radiusSteps.size,
                        onProgressChange = { index ->
                            onEvent(
                                CreatePlaceScreenUiEvent.OnRadiusChanged(
                                    CreatePlaceScreenState.radiusForIndex(index)
                                )
                            )
                        }
                    )
                }
            }

            if (state.hasSatelliteMode) {
                Image(
                    painter = if (!state.isSatelliteModeActive) {
                        MR.images.image_map_type_satellite.asPainterResource()
                    } else if (isSystemInDarkTheme()) {
                        MR.images.image_map_type_default_dark.asPainterResource()
                    } else {
                        MR.images.image_map_type_default_light.asPainterResource()
                    },
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 16.dp, end = 16.dp, start = 16.dp)
                        .size(40.dp)
                        .clickable { onEvent(CreatePlaceScreenUiEvent.OnMapModeSwitch) }
                        .background(
                            color = AppColors.accentDark,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            PlaceAddressSearchComponent(
                modifier = Modifier
                    .fillMaxSize()
                    .conditional(state.addressUiMode == CreatePlaceAddressSearchUiMode.FULLY_EXPANDED) {
                        clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            focusManager.clearFocus()
                            onEvent(CreatePlaceScreenUiEvent.ChangeSearchFieldFocus(isFocused = false))
                        }
                    }
                    .padding(4.dp),
                focusManager = focusManager,
                focusRequester = focusRequester,
                uiMode = state.addressUiMode,
                isLoading = state.isAddressLoading,
                address = state.address,
                suggestions = state.addressSuggestions,
                onEvent = onEvent
            )
        }

        if (!isSmallScreen) {
            Text(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(),
                style = MaterialTheme.typography.caption.copy(
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                ),
                text = state.hintText
            )

            Spacer(modifier = Modifier.size(12.dp))
        } else {
            Spacer(modifier = Modifier.size(8.dp))
        }

    }
}

@Composable
private fun PlaceAddressSearchComponent(
    modifier: Modifier,
    focusManager: FocusManager,
    focusRequester: FocusRequester,
    uiMode: CreatePlaceAddressSearchUiMode,
    isLoading: Boolean,
    address: TextFieldValue,
    suggestions: List<PlaceAddressSuggestion>,
    onEvent: (CreatePlaceScreenUiEvent) -> Unit
) {
    Box(
        modifier = modifier
            .background(
                color = animateColorAsState(
                    if (uiMode == CreatePlaceAddressSearchUiMode.FULLY_EXPANDED) {
                        MaterialTheme.colors.background
                    } else {
                        Color.Transparent
                    }
                ).value,
                shape = RoundedCornerShape(15.dp)
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = animateColorAsState(
                            if (uiMode != CreatePlaceAddressSearchUiMode.HIDDEN) {
                                MaterialTheme.colors.background
                            } else {
                                Color.Transparent
                            }
                        ).value,
                        shape = RoundedCornerShape(12.dp)
                    ),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Top
            ) {
                AnimatedVisibility(
                    modifier = Modifier.weight(1f),
                    visible = uiMode != CreatePlaceAddressSearchUiMode.HIDDEN
                ) {
                    MainInputBasicField(
                        modifier = Modifier
                            .padding(start = 4.dp, end = 8.dp)
                            .padding(bottom = 8.dp, top = 4.dp)
                            .height(height = 48.dp)
                            .focusRequester(focusRequester),
                        textValue = address,
                        hint = stringResource(MR.strings.places_create_address_placeholder),
                        focusManager = focusManager,
                        hasError = false,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.Words,
                        trailingIcon = if (address.text.isNotEmpty()) {
                            {
                                Icon(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable {
                                            onEvent(
                                                CreatePlaceScreenUiEvent.OnAddressChanged(
                                                    TextFieldValue(text = "")
                                                )
                                            )
                                        }
                                        .clip(shape = CircleShape)
                                        .background(
                                            color = MaterialTheme.colors.primary,
                                            shape = CircleShape
                                        )
                                        .padding(4.dp),
                                    painter = CoreAppIcons.Close.asPainterResource(),
                                    contentDescription = null,
                                    tint = MaterialTheme.colors.onPrimary
                                )
                            }
                        } else {
                            null
                        },
                        onInputChange = {
                            onEvent(CreatePlaceScreenUiEvent.OnAddressChanged(it))
                        }
                    )
                }

                val buttonColor = if (uiMode == CreatePlaceAddressSearchUiMode.HIDDEN) {
                    MaterialTheme.colors.background
                } else {
                    MaterialTheme.colors.surface
                }
                val buttonShape = RoundedCornerShape(14.dp)
                Box(
                    modifier = Modifier
                        .padding(
                            top = animateDpAsState(if (uiMode == CreatePlaceAddressSearchUiMode.HIDDEN) 8.dp else 4.dp).value,
                            bottom = 8.dp,
                            end = animateDpAsState(if (uiMode == CreatePlaceAddressSearchUiMode.HIDDEN) 8.dp else 4.dp).value
                        )
                        .size(48.dp)
                        .clickable { onEvent(CreatePlaceScreenUiEvent.ToggleSearchFieldFocus) }
                        .conditional(uiMode == CreatePlaceAddressSearchUiMode.HIDDEN) {
                            dropShadow(
                                shape = buttonShape,
                                shadow = Shadow(
                                    radius = 16.dp,
                                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.2f),
                                )
                            )
                        }
                        .clip(shape = buttonShape)
                        .background(
                            color = animateColorAsState(buttonColor).value,
                            shape = buttonShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isLoading,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = contentColorFor(buttonColor)
                        )
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isLoading,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Icon(
                            modifier = Modifier,
                            painter = if (uiMode == CreatePlaceAddressSearchUiMode.HIDDEN) {
                                CoreAppIcons.Search.asPainterResource()
                            } else {
                                CoreAppIcons.Close.asPainterResource()
                            },
                            contentDescription = null,
                            tint = contentColorFor(buttonColor)
                        )
                    }
                }
            }

            AnimatedVisibility(uiMode == CreatePlaceAddressSearchUiMode.FULLY_EXPANDED) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    this.items(suggestions) { suggestion ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clickable {
                                    onEvent(
                                        CreatePlaceScreenUiEvent.OnAddressSuggestionSelected(
                                            suggestion = suggestion
                                        )
                                    )
                                },
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (suggestion.title.isNotBlank()) {
                                Text(
                                    text = suggestion.title,
                                    style = MaterialTheme.typography.subtitle2
                                )
                            }

                            if (suggestion.info.isNotBlank()) {
                                Text(
                                    text = suggestion.info,
                                    style = MaterialTheme.typography.caption
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}