package app.paralino.android.ui.places.list

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.paralino.android.R
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.components.DefaultEmptyState
import app.paralino.android.ui.components.ImageModelImage
import app.paralino.android.ui.map.rememberMapViewPlacesOverviewState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.ui.settings.SettingsBigHeader
import app.paralino.android.ui.shared.errorString
import app.paralino.android.ui.theme.AppTheme
import app.paralino.core.models.Payload
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.places.list.PlaceListItemData
import app.paralino.core.presentation.places.list.PlacesListScreenState
import app.paralino.core.presentation.places.list.PlacesScreenEvent
import app.paralino.core.utils.Emojis
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun PlacesScreen(
    appState: MainAppState,
    groupId: String,
    vm: PlacesViewModel = getViewModel { parametersOf(groupId) },
) {

    val uiState by vm.uiState.collectAsState(initial = Payload.Loading)

    val context = LocalContext.current
    LaunchedEffect(key1 = context) {
        vm.events.collectLatest { event ->
            when (event) {
                is PlacesScreenEvent.Error -> {
                    appState.showSnackbar(event.error.errorString(context))
                }
                null -> {
                    /* do nothing */
                }
            }
        }
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
            DefaultEmptyState(content = stringResource(id = R.string.error_load_data_generic))
        }
        is Payload.Success -> {
            PlacesScreen(
                state = state.data,
                onScreenClose = {
                    appState.navController.popBackStack()
                },
                onPlaceClick = {
                    appState.navController.navigate(AppScreen.DashboardRoot.PlaceCreate.route(groupId = groupId, placeId = it.id))
                },
                onCreatePlace = {
                    appState.navController.navigate(AppScreen.DashboardRoot.PlaceCreate.route(groupId = groupId, placeId = null))
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
    onScreenClose: () -> Unit,
    onCreatePlace: () -> Unit
) {

    val mapViewState = rememberMapViewPlacesOverviewState(darkMode = isSystemInDarkTheme())

    LaunchedEffect(state) {
        delay(50)
        mapViewState.updateMarkers(state.groupId, state.places)
    }

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
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = {
                    Text(stringResource(id = R.string.places_list_add_button))
                },
                icon = {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                },
                backgroundColor = MaterialTheme.colors.primary,
                elevation = FloatingActionButtonDefaults.elevation(2.dp, 4.dp, 3.dp, 3.dp),
                onClick = onCreatePlace
            )
        },
        isFloatingActionButtonDocked = true
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = it.calculateTopPadding(),
                bottom = it.calculateBottomPadding() + 100.dp,
                start = 20.dp,
                end = 20.dp
            )
        ) {

            this.item(key = "main_header") {
                SettingsBigHeader(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    name = stringResource(id = R.string.places_list_screen_title)
                )
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
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colors.background,
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = {
                                mapViewState.initMap()
                                mapViewState.mapAndroidView
                            },
                            update = { }
                        )
                    }
                }
            }

            this.itemsIndexed(
                items = state.places,
                key = { index, item -> item.id }
            ) { index, item ->

                if (index == 0)
                    Spacer(modifier = Modifier.size(20.dp))

                PlaceListItem(
                    modifier = Modifier.animateItemPlacement(),
                    state = item,
                    onClick = { onPlaceClick(item) },
                )
            }

            if (state.places.isEmpty()) {
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

}

@Composable
fun PlaceListItem(
    modifier: Modifier = Modifier,
    state: PlaceListItemData,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.surface)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ImageModelImage(
            imageModel = state.image,
            modifier = Modifier
                .size(46.dp)
                .border(
                    0.5.dp,
                    MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled),
                    RoundedCornerShape(10.dp)
                )
        )

        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = state.name,
            style = MaterialTheme.typography.h6,
            modifier = Modifier
        )
    }
}

@Preview("place_item")
@Composable
fun PlaceListItemPreview() {
    AppTheme {
        PlaceListItem(
            state = PlaceListItemData(
                id = "",
                name = "Home",
                image = ImageModel.emoji(Emojis.group1.first()),
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
                name = "Place $it",
                image = ImageModel.emoji(Emojis.group1.random()),
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
                places = items
            ),
            onCreatePlace = {},
            onPlaceClick = {},
            onScreenClose = {}
        )
    }
}