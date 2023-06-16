package app.paralino.android.ui.places.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.paralino.android.R
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.map.rememberMapViewPlaceCreateState
import app.paralino.android.ui.shared.errorString
import app.paralino.android.ui.theme.AppIcons
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.extensions.annotatedBold
import app.paralino.core.utils.extensions.safeCast
import app.paralino.core.models.Payload
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.presentation.places.create.CreatePlaceScreenEvent
import app.paralino.core.presentation.places.create.CreatePlaceScreenState
import app.paralino.core.presentation.places.create.CreatePlaceViewModel
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.BottomSheetsScreen
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.components.LargeSeekbarSwitch
import app.paralino.core.ui.components.LargeSeekbarSwitchState
import app.paralino.core.ui.components.LargeSwitchDefaults
import app.paralino.core.ui.components.MainInputField
import app.paralino.core.ui.components.rememberSeekbarState
import app.paralino.core.utils.Emojis
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CreatePlaceScreen(
    appState: MainAppState,
    groupId: String,
    placeId: String?,
    vm: CreatePlaceViewModel = getViewModel { parametersOf(groupId, placeId) },
) {

    val state by vm.uiState.collectAsState(initial = Payload.Loading)

    val context = LocalContext.current
    LaunchedEffect(key1 = context) {
        vm.events.collectLatest { event ->
            when (event) {
                is CreatePlaceScreenEvent.Error -> {
                    appState.showSnackbar(event.error.errorString(context))
                }
                is CreatePlaceScreenEvent.MissingLocation -> {
                    appState.showSnackbar(context.getString(R.string.places_create_error_no_place))
                }
                is CreatePlaceScreenEvent.MissingInfo -> {
                    appState.showSnackbar(context.getString(R.string.form_generic_validation))
                }
                is CreatePlaceScreenEvent.PlaceUpdated -> {
                    delay(400)
                    appState.navController.popBackStack()
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
                        Text(stringResource(id = R.string.places_create_screen_title))
                    } else if (state.data?.isNewPlace == false) {
                        Text(stringResource(id = R.string.places_edit_screen_title))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { appState.navController.popBackStack() }) {
                        Icon(
                            painter = AppIcons.Close,
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
                            icon = if (finalState.data.isNewPlace) null else AppIcons.Save,
                            onClick = { vm.createPlace() }
                        ) {
                            if (finalState.data.isNewPlace) {
                                Text(text = stringResource(id = R.string.places_create_button_create))
                            } else {
                                Text(text = stringResource(id = R.string.button_save))
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
                DefaultEmptyState(content = stringResource(id = R.string.error_load_data_generic))
            }
            is Payload.Success -> {
                CreatePlaceScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it),
                    state = finalState.data,
                    onImageClick = {
                        appState.uiScope.launch {
                            appState.showBottomSheetForResult(BottomSheetsScreen.EmojiPicker(Emojis.groupEmojis))
                                .safeCast<ImageModel>()?.let {
                                    vm.updateImage(it)
                                }
                        }
                    },
                    onNameChange = { vm.updateName(it) },
                    onLocationChanged = { lat, lng -> vm.updatePosition(lat, lng) },
                    onRadiusChanged = { vm.updateRadius(it) },
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
fun CreatePlaceScreen(
    modifier: Modifier = Modifier,
    state: CreatePlaceScreenState,
    onImageClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onLocationChanged: (lat: Double, lng: Double) -> Unit,
    onRadiusChanged: (radiusMeters: Int) -> Unit
) {

    val mapViewState = rememberMapViewPlaceCreateState(darkMode = isSystemInDarkTheme())
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

    LaunchedEffect(state.lat, state.lng, state.image) {
        delay(50)
        mapViewState.updateMarkers(state)
    }

    LaunchedEffect(state.radiusMeters) {
        mapViewState.updateRadius(state.radiusMeters)
    }

    var userScroll by remember {
        mutableStateOf(true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState(), enabled = userScroll)
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
                hint = stringResource(id = R.string.login_placeholder_user_name),
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
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = {
                        mapViewState.initMap()
                        mapViewState.onMapLongPress = { lat, lng ->
                            onLocationChanged(lat, lng)
                            true
                        }
                        mapViewState.onMapTouchEnd = {
                            userScroll = true
                        }
                        mapViewState.onMapTouch = {
                            userScroll = false
                            false
                        }
                        mapViewState.onMarkerClick = {
                            true
                        }
                        mapViewState.mapAndroidView
                    },
                    update = { }
                )
            }
        }

        Text(
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth(),
            style = MaterialTheme.typography.caption,
            textAlign = TextAlign.Center,
            text = stringResource(id = R.string.places_create_location_hint).annotatedBold()
        )

        Spacer(modifier = Modifier.padding(20.dp))

        LargeSeekbarSwitch(
            modifier = Modifier
                .fillMaxWidth()
                .height(LargeSwitchDefaults.minHeight),
            icon = AppIcons.Adjust,
            title = stringResource(id = R.string.places_create_seekbar_info),
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