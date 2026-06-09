package app.paralino.core.ui.privacyzones

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
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
import app.paralino.core.presentation.places.create.PlaceAddressSearchUiMode
import app.paralino.core.presentation.privacyzones.create.CreatePrivacyZoneScreenEvent
import app.paralino.core.presentation.privacyzones.create.CreatePrivacyZoneScreenState
import app.paralino.core.presentation.privacyzones.create.CreatePrivacyZoneScreenStep
import app.paralino.core.presentation.privacyzones.create.CreatePrivacyZoneScreenUiEvent
import app.paralino.core.presentation.privacyzones.create.CreatePrivacyZoneViewModel
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.ScaffoldBottomBarButton
import app.paralino.core.ui.components.dialogs.LoadingDialog
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.theme.LocalScreenDimensionType
import app.paralino.core.utils.Emojis
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun CreatePrivacyZoneScreenCore(
    appState: CoreUiState,
    vm: CreatePrivacyZoneViewModel,
    onNavigateBack: () -> Unit,
    onBottomSheetAction: (BottomSheetPickerType.EmojiPickerBottomSheet) -> Unit,
    mapContent: @Composable (CreatePrivacyZoneScreenState) -> Unit,
) {
    MessageDispatcherEffect(vm, appState)
    val screenDimensionType = LocalScreenDimensionType.current
    LaunchedEffect(key1 = screenDimensionType.isReady) {
        vm.onEvent(CreatePrivacyZoneScreenUiEvent.OnUiReady(size = screenDimensionType))
    }

    val uiState by vm.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val inputMethod = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                is CreatePrivacyZoneScreenEvent.MissingLocation -> {
                    vm.showSnackbar(MR.strings.privacy_zones_create_error_no_location.localizedString())
                }

                is CreatePrivacyZoneScreenEvent.MissingInfo -> {
                    vm.showSnackbar(MR.strings.form_generic_validation.localizedString())
                }

                is CreatePrivacyZoneScreenEvent.ZoneUpdated -> {
                    delay(400)
                    onNavigateBack()
                }

                is CreatePrivacyZoneScreenEvent.RequestKeyboardFocusOnAddressInput -> {
                    scope.launch {
                        delay(100)
                        inputMethod?.show()
                        try {
                            focusRequester.requestFocus()
                        } catch (_: Exception) {
                        }
                    }
                }

                null -> {}
            }
        }
    }

    val dialog = uiState.data?.alertDialog
    dialog?.Show {
        vm.onEvent(CreatePrivacyZoneScreenUiEvent.DialogAction(AlertDialogAction(dialog, it)))
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
                                finalState.data.screenStep == CreatePrivacyZoneScreenStep.NAME_SUGGESTION ||
                                finalState.data.addressUiMode == PlaceAddressSearchUiMode.HIDDEN
                    ) {
                        ScaffoldBottomBarButton(
                            buttonState = AppButtonState.loadingStateWithCheck(finalState.data.actionLoading),
                            buttonText = when (finalState.data.screenStep) {
                                CreatePrivacyZoneScreenStep.NAME_SUGGESTION -> {
                                    stringResource(MR.strings.button_next)
                                }

                                CreatePrivacyZoneScreenStep.FULL -> {
                                    if (finalState.data.isNewZone) {
                                        stringResource(MR.strings.privacy_zones_create_button_create)
                                    } else {
                                        stringResource(MR.strings.button_save)
                                    }
                                }
                            },
                            buttonIcon = null,
                            onClick = { vm.onEvent(CreatePrivacyZoneScreenUiEvent.CreatePrivacyZone) }
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
                CreatePrivacyZoneScreenContent(
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
private fun CreatePrivacyZoneScreenContent(
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester,
    state: CreatePrivacyZoneScreenState,
    onImageClick: () -> Unit,
    onEvent: (CreatePrivacyZoneScreenUiEvent) -> Unit,
    mapContent: @Composable (CreatePrivacyZoneScreenState) -> Unit,
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
    ) {
        CreatePrivacyZoneMapStep(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            focusManager = focusManager,
            focusRequester = focusRequester,
            state = state,
            onImageClick = onImageClick,
            onEvent = onEvent,
            mapContent = mapContent
        )

        AnimatedVisibility(
            visible = state.screenStep == CreatePrivacyZoneScreenStep.NAME_SUGGESTION,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            CreatePrivacyZoneNameSuggestionStep(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background)
                    .padding(horizontal = 20.dp),
                focusManager = focusManager,
                shakeImage = state.isNewZone,
                image = state.image,
                name = state.name,
                hasNameError = state.hasNameError,
                suggestions = state.nameSuggestions,
                onImageClick = onImageClick,
                onNameChanged = { onEvent(CreatePrivacyZoneScreenUiEvent.OnNameChanged(it)) },
                onSuggestionSelected = {
                    onEvent(CreatePrivacyZoneScreenUiEvent.OnNameSuggestionSelected(it))
                }
            )
        }
    }
}
