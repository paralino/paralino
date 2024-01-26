package app.paralino.core.ui.places

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.presentation.places.create.CreatePlaceScreenEvent
import app.paralino.core.presentation.places.create.CreatePlaceScreenState
import app.paralino.core.presentation.places.create.CreatePlaceViewModel
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.BottomSheetsScreen
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.components.LargeSeekbarSwitch
import app.paralino.core.ui.components.LargeSeekbarSwitchState
import app.paralino.core.ui.components.LargeSwitchDefaults
import app.paralino.core.ui.components.MainInputField
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.rememberSeekbarState
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.annotatedBold
import app.paralino.core.utils.extensions.errorString
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
fun CreatePlaceScreenCore(
    appState: CoreUiState,
    vm: CreatePlaceViewModel,
    userScrollEnabled: Boolean,
    onNavigateBack: () -> Unit,
    onBottomSheetAction: (BottomSheetsScreen?) -> Unit,
    mapContent: @Composable (CreatePlaceScreenState) -> Unit,
) {

    MessageDispatcherEffect(vm, appState)

    val state by vm.uiState.collectAsState(initial = Payload.Loading)

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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.background,
                elevation = 1.dp,
                title = {
                    if (state.data?.isNewPlace == true) {
                        Text(stringResource(MR.strings.places_create_screen_title))
                    } else if (state.data?.isNewPlace == false) {
                        Text(stringResource(MR.strings.places_edit_screen_title))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack() }) {
                        Icon(
                            painter = CoreAppIcons.Close.asPainterResource(),
                            contentDescription = null
                        )
                    }
                }
            )
        },
        bottomBar = {
            when (val finalState = state) {
                is Payload.Error -> {}
                Payload.Loading -> {}
                is Payload.Success -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colors.background)
                            .padding(bottom = 20.dp, top = 8.dp)
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FilledButton(
                            modifier = Modifier,
                            style = ButtonStyle.BIG,
                            state = AppButtonState.loadingStateWithCheck(finalState.data.actionLoading),
                            icon = if (finalState.data.isNewPlace) null else CoreAppIcons.Save.asPainterResource(),
                            onClick = { vm.createPlace() }
                        ) {
                            if (finalState.data.isNewPlace) {
                                Text(text = stringResource(MR.strings.places_create_button_create))
                            } else {
                                Text(text = stringResource(MR.strings.button_save))
                            }
                        }
                    }
                }
            }
        },
    ) {

        when (val finalState = state) {
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
                    userScrollEnabled = userScrollEnabled,
                    onImageClick = {
                        onBottomSheetAction(BottomSheetsScreen.EmojiPicker(Emojis.groupEmojis))
                    },
                    onNameChange = { vm.updateName(it) },
                    onRadiusChanged = { vm.updateRadius(it) },
                    mapContent = mapContent
                )

                AnimatedVisibility(
                    finalState.data.fullLoading,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color = MaterialTheme.colors.background.copy(alpha = 0.5f))
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

}

@Composable
private fun CreatePlaceScreenCore(
    modifier: Modifier = Modifier,
    state: CreatePlaceScreenState,
    userScrollEnabled: Boolean,
    onImageClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onRadiusChanged: (radiusMeters: Int) -> Unit,
    mapContent: @Composable (CreatePlaceScreenState) -> Unit,
) {

    val seekbarState by rememberSeekbarState(
        key = state,
        LargeSeekbarSwitchState.SeekbarSwitchState(
            seekbarState = LargeSeekbarSwitchState.Seekbar(
                seekbarProgress = state.currentStep + 1,
                seekbarRange = IntRange(1, CreatePlaceScreenState.steps.size)
            ),
            switchState = null
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState(), enabled = userScrollEnabled)
            .padding(horizontal = 20.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 30.dp)
                .height(60.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ImageModelImage(
                imageModel = state.image,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(60.dp)
                    .clickable { onImageClick() }
            )

            Spacer(modifier = Modifier.size(8.dp))

            MainInputField(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                text = state.name,
                hint = stringResource(MR.strings.login_placeholder_user_name),
                hasError = state.hasNameError,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
                onInputChange = onNameChange
            )
        }

        // map
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .padding(top = 30.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colors.background,
            border = BorderStroke(6.dp, MaterialTheme.colors.surface),
            elevation = 12.dp
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colors.background,
            ) {
                mapContent(state)
            }
        }

        Text(
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth(),
            style = MaterialTheme.typography.caption,
            textAlign = TextAlign.Center,
            text = stringResource(MR.strings.places_create_location_hint).annotatedBold()
        )

        Spacer(modifier = Modifier.padding(20.dp))

        LargeSeekbarSwitch(
            modifier = Modifier
                .fillMaxWidth()
                .height(LargeSwitchDefaults.minHeight),
            icon = CoreAppIcons.Adjust.asPainterResource(),
            title = stringResource(MR.strings.places_create_seekbar_info),
            description = "${state.radiusMeters}m",
            focusDescription = true,
            state = seekbarState,
            onProgressChange = {
                onRadiusChanged(CreatePlaceScreenState.radiusForIndex(it))
            }
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}