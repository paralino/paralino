package app.paralino.core.ui.places

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.presentation.places.create.CreatePlaceScreenEvent
import app.paralino.core.presentation.places.create.CreatePlaceScreenState
import app.paralino.core.presentation.places.create.CreatePlaceScreenUiEvent
import app.paralino.core.presentation.places.create.CreatePlaceViewModel
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.BottomSheetPickerType
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.EmbeddedSlider
import app.paralino.core.ui.components.EmbeddedSliderSize
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.components.MainInputField
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.ScaffoldBottomBarButton
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.LoadingDialog
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.errorString
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
fun CreatePlaceScreenCore(
    appState: CoreUiState,
    vm: CreatePlaceViewModel,
    onNavigateBack: () -> Unit,
    onBottomSheetAction: (BottomSheetPickerType.EmojiPickerBottomSheet) -> Unit,
    mapContent: @Composable (CreatePlaceScreenState) -> Unit,
) {

    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState(initial = Payload.Loading)

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                is CreatePlaceScreenEvent.Error -> {
                    vm.showSnackbar(event.error.errorString())
                }

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
                    ScaffoldBottomBarButton(
                        buttonState = AppButtonState.loadingStateWithCheck(finalState.data.actionLoading),
                        buttonText = if (finalState.data.isNewPlace) {
                            stringResource(MR.strings.places_create_button_create)
                        } else {
                            stringResource(MR.strings.button_save)
                        },
                        buttonIcon = if (finalState.data.isNewPlace) null else CoreAppIcons.Save.asPainterResource(),
                        onClick = { vm.onEvent(CreatePlaceScreenUiEvent.CreatePlace) }
                    )
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
                    state = finalState.data,
                    onImageClick = {
                        onBottomSheetAction(BottomSheetPickerType.EmojiPickerBottomSheet(Emojis.groupEmojis))
                    },
                    onNameChange = { vm.updateName(it) },
                    onRadiusChanged = { vm.updateRadius(it) },
                    mapContent = mapContent
                )

                if (finalState.data.fullLoading) {
                    LoadingDialog(text = stringResource(MR.strings.login_step_generic_loading), progress = null)
                }
            }
        }
    }
}

@Composable
private fun CreatePlaceScreenCore(
    modifier: Modifier = Modifier,
    state: CreatePlaceScreenState,
    onImageClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onRadiusChanged: (radiusMeters: Int) -> Unit,
    mapContent: @Composable (CreatePlaceScreenState) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Column(
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp)
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically
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
                hint = stringResource(MR.strings.login_placeholder_user_name),
                focusManager = focusManager,
                hasError = state.hasNameError,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
                onInputChange = onNameChange
            )
        }

        // map
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colors.surface,
            border = BorderStroke(6.dp, MaterialTheme.colors.surface),
            elevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colors.surface)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .padding(6.dp),
                    shape = RoundedCornerShape(15.dp),
                    color = MaterialTheme.colors.surface,
                ) {
                    mapContent(state)
                }

                val sliderSize = remember {
                    EmbeddedSliderSize(minStepWidth = 120.dp)
                }
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
                    onProgressChange = {
                        onRadiusChanged(CreatePlaceScreenState.radiusForIndex(it))
                    }
                )
            }
        }

        Text(
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth(),
            style = MaterialTheme.typography.caption,
            textAlign = TextAlign.Center,
            text = state.hintText
        )

        Spacer(modifier = Modifier.size(30.dp))
    }
}