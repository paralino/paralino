package app.paralino.android.ui.places.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.paralino.android.R
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.components.DefaultEmptyState
import app.paralino.android.ui.components.MainAlertDialog
import app.paralino.android.ui.map.rememberMapViewPlaceCreateState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.ui.places.list.PlaceListItem
import app.paralino.android.ui.settings.SettingsBigHeader
import app.paralino.android.ui.shared.errorString
import app.paralino.android.ui.theme.AppColors
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.presentation.places.detail.PlaceDetailScreenEvent
import app.paralino.core.presentation.places.detail.PlaceDetailScreenState
import app.paralino.core.presentation.places.list.PlaceListItemData
import app.paralino.core.presentation.rawdata.RawPresentationDataType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun PlaceDetailScreen(
    appState: MainAppState,
    groupId: String,
    placeId: String,
    vm: PlaceDetailViewModel = getViewModel { parametersOf(groupId, placeId) },
) {

    val state by vm.uiState.collectAsState(initial = Payload.Loading)
    var optionMenuExpanded by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf<MainAlertDialog?>(null) }

    val context = LocalContext.current
    LaunchedEffect(key1 = context) {
        vm.events.collectLatest { event ->
            when (event) {
                is PlaceDetailScreenEvent.Error -> {
                    appState.showSnackbar(event.error.errorString(context))
                }
                is PlaceDetailScreenEvent.PlaceDeleted -> {
                    appState.navController.popBackStack()
                }
                null -> {
                    /* do nothing */
                }
            }
        }
    }

    when (val dialog = showDialog) {
        is MainAlertDialog.DeletePlace -> {
            dialog.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk)
                    vm.deletePlace()
            }
        }
        else -> {}
    }


    Scaffold(
        topBar = {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.background,
                elevation = 1.dp,
                title = {
                    Text(state.data?.groupName ?: "")
                },
                navigationIcon = {
                    IconButton(onClick = { appState.navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            appState.navController.navigate(
                                AppScreen.DashboardRoot.RawDataScreen.route(
                                    RawPresentationDataType.GroupPlacePresentation(
                                        placeId = state.data!!.placeId
                                    )
                                )
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            tint = AppColors.accent,
                            contentDescription = null
                        )
                    }
                    if (state.data?.canEdit == true) {
                        IconButton(
                            onClick = {
                                appState.navController.navigate(
                                    AppScreen.DashboardRoot.PlaceCreate.route(
                                        groupId = state.data!!.groupId,
                                        placeId = state.data!!.placeId
                                    )
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null
                            )
                        }
                        IconButton(onClick = { optionMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = null
                            )
                        }

                        DropdownMenu(
                            expanded = optionMenuExpanded,
                            onDismissRequest = { optionMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                onClick = {
                                    optionMenuExpanded = false
                                    showDialog = MainAlertDialog.DeletePlace
                                }
                            ) {
                                Text(text = stringResource(id = R.string.place_list_menu_delete))
                            }
                        }
                    }
                }
            )
        }
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
                PlaceDetailScreen(
                    state = finalState.data,
                    paddingValues = it,
                    onEnterChange = {
                        vm.toggleEnterSettings(it)
                    },
                    onExitChange = {
                        vm.toggleExitSettings(it)
                    }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaceDetailScreen(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    state: PlaceDetailScreenState,
    onEnterChange: (String) -> Unit,
    onExitChange: (String) -> Unit
) {

    val mapViewState = rememberMapViewPlaceCreateState(darkMode = isSystemInDarkTheme())

    LaunchedEffect(state) {
        delay(50)
        mapViewState.updateMarkers(state)
    }

    var userScroll by remember {
        mutableStateOf(true)
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = paddingValues.calculateTopPadding(),
            bottom = paddingValues.calculateBottomPadding(),
            start = 20.dp,
            end = 20.dp
        ),
        userScrollEnabled = userScroll
    ) {
        if (state.settings.isNotEmpty()) {

            this.item(key = "main_header") {
                PlaceListItem(
                    modifier = Modifier.padding(top = 26.dp),
                    state = PlaceListItemData(
                        id = state.placeId,
                        name = state.name,
                        image = state.image,
                        private = state.private,
                        lat = 0.0,
                        lng = 0.0,
                        radiusMeters = 0
                    )
                )
            }

            this.item(key = "map") {
                // map
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .padding(top = 26.dp),
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
                                mapViewState.onMapTouchEnd = {
                                    userScroll = true
                                }
                                mapViewState.onMapTouch = {
                                    userScroll = false
                                    false
                                }
                                mapViewState.mapAndroidView
                            },
                            update = { }
                        )
                    }
                }
            }
        }

        this.item(key = "spacer") {
            Spacer(modifier = Modifier.size(40.dp))
        }

        this.itemsIndexed(
            items = state.settings,
            key = { index, item -> item.memberId }
        ) { index, item ->
            PlaceDetailUserListItem(
                modifier = Modifier.animateItemPlacement(),
                state = item,
                onEnterChange = onEnterChange,
                onExitChange = onExitChange,
            )
        }

        if (state.settings.isEmpty()) {
            this.item {
                DefaultEmptyState(
                    content = stringResource(id = R.string.places_list_screen_empty_info),
                    icon = Icons.Rounded.Home,
                    buttonText = null
                ) { }
            }
        }
    }

}