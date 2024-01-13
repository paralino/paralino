package app.paralino.android.ui.places.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.FloatingActionButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.paralino.android.notification.rememberNotificationsPermissionHandler
import app.paralino.android.ui.MainAppState
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.android.ui.map.SimplePlaceMapComposable
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.ui.shared.errorString
import app.paralino.android.ui.theme.AppIcons
import app.paralino.core.MR
import app.paralino.core.models.Payload
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.places.list.PlaceListItemData
import app.paralino.core.presentation.places.list.PlacesListScreenState
import app.paralino.core.presentation.places.list.PlacesScreenEvent
import app.paralino.core.presentation.places.list.PlacesViewModel
import app.paralino.core.ui.components.ConditionErrorItem
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.PlanLimitCount
import app.paralino.core.ui.components.SettingsBigHeader
import app.paralino.core.ui.places.PlaceListItem
import app.paralino.core.ui.theme.AppTheme
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.Emojis
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun PlacesScreen(
    appState: MainAppState,
    groupId: String,
    vm: PlacesViewModel = getViewModel { parametersOf(groupId) },
) {

    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState(initial = Payload.Loading)
    var showDialog by remember { mutableStateOf<MainAlertDialog?>(null) }

    val notificationPermissionHandler = rememberNotificationsPermissionHandler(
        appState = appState,
        messageDispatcher = vm.messageDispatcher,
        locationStatusProvider = vm.locationStatusProvider
    )

    val context = LocalContext.current
    LaunchedEffect(key1 = context) {
        vm.events.collectLatest { event ->
            when (event) {
                is PlacesScreenEvent.Error -> {
                    vm.showSnackbar(event.error.errorString(context))
                }

                PlacesScreenEvent.AddPlace -> {
                    appState.navController.navigate(
                        AppScreen.MainHome.PlaceCreate.route(
                            groupId = groupId,
                            placeId = null
                        )
                    )
                }

                is PlacesScreenEvent.GroupPlacesLimitReached -> {
                    showDialog = MainAlertDialog.PlanLimit(
                        context.getString(MR.strings.plan_limit_places_alert_info.resourceId),
                        limit = event.limit
                    )
                }

                null -> { /* do nothing */
                }
            }
        }
    }

    when (val dialog = showDialog) {
        is MainAlertDialog.PlanLimit -> {
            dialog.Show {
                showDialog = null
            }
        }

        else -> {}
    }

    when (val state = uiState) {
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
            DefaultEmptyState(content = stringResource(id = MR.strings.error_load_data_generic.resourceId))
        }

        is Payload.Success -> {
            PlacesScreen(
                state = state.data,
                onScreenClose = {
                    appState.navController.popBackStack()
                },
                onPlaceClick = {
                    appState.navController.navigate(
                        AppScreen.MainHome.PlaceDetail.route(
                            groupId = groupId,
                            placeId = it.id
                        )
                    )
                },
                onCreatePlace = {
                    vm.tryCreatePlace()
                },
                onErrorItemClick = {
                    notificationPermissionHandler.askNotificationPermission()
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlacesScreen(
    state: PlacesListScreenState,
    onPlaceClick: (PlaceListItemData) -> Unit,
    onErrorItemClick: (ConditionErrorType) -> Unit,
    onScreenClose: () -> Unit,
    onCreatePlace: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.background,
                elevation = 1.dp,
                title = { Text(state.groupName) },
                navigationIcon = {
                    IconButton(onClick = onScreenClose) {
                        Icon(
                            painter = AppIcons.Close,
                            contentDescription = null
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (state.places.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    text = {
                        Text(stringResource(id = MR.strings.places_list_add_button.resourceId))
                    },
                    icon = {
                        Icon(painter = AppIcons.Add, contentDescription = null)
                    },
                    backgroundColor = MaterialTheme.colors.primary,
                    elevation = FloatingActionButtonDefaults.elevation(2.dp, 4.dp, 3.dp, 3.dp),
                    onClick = onCreatePlace
                )
            }
        },
        isFloatingActionButtonDocked = true
    ) {
        var userScroll by remember {
            mutableStateOf(true)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = it.calculateTopPadding(),
                bottom = it.calculateBottomPadding() + 100.dp,
                start = 20.dp,
                end = 20.dp
            ),
            userScrollEnabled = userScroll
        ) {
            if (state.places.isNotEmpty()) {

                this.item(key = "main_header") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsBigHeader(
                            name = stringResource(id = MR.strings.places_list_screen_title.resourceId)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        PlanLimitCount(
                            modifier = Modifier.padding(top = 4.dp),
                            count = state.places.size,
                            limit = state.placesLimit
                        )
                    }
                }

                this.items(items = state.errors, key = { it.toString() }) {
                    ConditionErrorItem(data = it) {
                        onErrorItemClick(it)
                    }
                }

                this.item(key = "map") {
                    // map
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
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
                            SimplePlaceMapComposable(
                                markers = state.markers,
                                onMapInteraction = { userScroll = !it },
                                onMapLongPress = { lat, lng -> })
                        }
                    }
                }
            }

            this.itemsIndexed(
                items = state.places,
                key = { index, item -> item.id }
            ) { index, item ->

                if (index == 0)
                    Spacer(modifier = Modifier.size(20.dp))
                else
                    Spacer(modifier = Modifier.size(8.dp))

                PlaceListItem(
                    modifier = Modifier.animateItemPlacement(),
                    state = item,
                    hasEndArrow = true,
                    onClick = { onPlaceClick(item) },
                )
            }

            if (state.places.isEmpty()) {
                this.item {
                    DefaultEmptyState(
                        content = stringResource(id = MR.strings.places_list_screen_empty_info.resourceId),
                        icon = AppIcons.MapsHomeWork,
                        buttonText = stringResource(id = MR.strings.places_create_button_create.resourceId),
                        buttonIcon = AppIcons.Add
                    ) {
                        onCreatePlace()
                    }
                }
            }
        }
    }
}

@Preview("place_item")
@Composable
fun PlaceListItemPreview() {
    AppTheme {
        PlaceListItem(
            state = PlaceListItemData(
                id = "",
                groupId = "",
                name = "Home",
                image = ImageModel.emoji(Emojis.lightBulb),
                private = false,
                lat = 0.0,
                lng = 0.0,
                radiusMeters = 1000
            )
        ) {}
    }
}

@Preview("places_screen")
@Composable
fun PlacesScreenPreview() {
    AppTheme {

        val items = (1..5).map {
            PlaceListItemData(
                id = "$it",
                groupId = "",
                name = "Place $it",
                image = ImageModel.emoji(Emojis.lightBulb),
                private = false,
                lat = 0.0,
                lng = 0.0,
                radiusMeters = 1000
            )
        }

        PlacesScreen(
            state = PlacesListScreenState(
                groupId = "",
                groupName = "Family",
                places = items,
                placesLimit = 10,
                errors = listOf(ConditionErrorType.PERMISSION_NOTIFICATIONS)
            ),
            onCreatePlace = {},
            onPlaceClick = {},
            onScreenClose = {},
            onErrorItemClick = {}
        )
    }
}