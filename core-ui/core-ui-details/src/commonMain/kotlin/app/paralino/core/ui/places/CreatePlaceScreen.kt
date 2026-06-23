package app.paralino.core.ui.places

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.common.BottomSheetPickerType
import app.paralino.core.presentation.places.create.CreatePlaceScreenEvent
import app.paralino.core.presentation.places.create.CreatePlaceScreenState
import app.paralino.core.presentation.places.create.CreatePlaceScreenStep
import app.paralino.core.presentation.places.create.CreatePlaceScreenUiEvent
import app.paralino.core.presentation.places.create.CreatePlaceViewModel
import app.paralino.core.presentation.places.create.PlaceAddressSearchUiMode
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.ScaffoldBottomBarButton
import app.paralino.core.ui.components.dialogs.LoadingDialog
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.groups.components.InitialGroupContentCreationComponent
import app.paralino.core.ui.theme.LocalScreenDimensionType
import app.paralino.core.utils.Emojis
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
                    AnimatedVisibility(
                        !screenDimensionType.isSmall ||
                                finalState.data.screenStep == CreatePlaceScreenStep.NAME_AND_IMAGE ||
                                finalState.data.addressUiMode == PlaceAddressSearchUiMode.HIDDEN
                    ) {
                        val buttonText = if (finalState.data.editOnlyCurrentStep) {
                            stringResource(MR.strings.button_save)
                        } else {
                            when (finalState.data.screenStep) {
                                CreatePlaceScreenStep.NAME_AND_IMAGE -> {
                                    stringResource(MR.strings.button_next)
                                }

                                CreatePlaceScreenStep.ADDRESS -> {
                                    if (finalState.data.isNewPlace) {
                                        stringResource(MR.strings.places_create_button_create)
                                    } else {
                                        stringResource(MR.strings.button_save)
                                    }
                                }
                            }
                        }

                        ScaffoldBottomBarButton(
                            buttonState = AppButtonState.loadingStateWithCheck(finalState.data.actionLoading),
                            buttonText = buttonText,
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
            visible = state.screenStep == CreatePlaceScreenStep.NAME_AND_IMAGE,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            InitialGroupContentCreationComponent(
                modifier = Modifier.fillMaxSize(),
                focusManager = focusManager,
                shakeImage = state.isNewPlace,
                image = state.image,
                name = state.name,
                namePlaceholder = null,
                maxTextLength = CreatePlaceScreenState.placeNameLengthLimit,
                hint = stringResource(MR.strings.places_create_name_placeholder),
                hasNameError = state.hasNameError,
                suggestions = state.nameSuggestions,
                bottomContent = {},
                onImageClick = onImageClick,
                onNameChanged = { onEvent(CreatePlaceScreenUiEvent.OnNameChanged(it)) },
                onSuggestionSelected = { onEvent(CreatePlaceScreenUiEvent.OnNameSuggestionSelected(it)) },
            )
        }

        AnimatedVisibility(
            visible = state.screenStep == CreatePlaceScreenStep.ADDRESS,
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
