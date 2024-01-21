package app.paralino.core.presentation.places.list

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
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.Payload
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.ConditionErrorItem
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.PlanLimitCount
import app.paralino.core.ui.components.SettingsBigHeader
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.places.PlaceListItem
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.extensions.errorString
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PlacesScreenCore(
    appState: CoreUiState,
    vm: PlacesViewModel,
    userScrollEnabled: Boolean,
    askNotificationPermission: () -> Unit,
    addNewPlace: () -> Unit,
    openPlaceDetails: (place: PlaceListItemData) -> Unit,
    onNavigateBack: () -> Unit,
    mapContent: @Composable (PlacesListScreenState) -> Unit
) {

    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState(initial = Payload.Loading)
    var showDialog by remember { mutableStateOf<MainAlertDialog?>(null) }

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                is PlacesScreenEvent.Error -> {
                    vm.showSnackbar(event.error.errorString())
                }

                PlacesScreenEvent.AddPlace -> {
                    addNewPlace()
                }

                is PlacesScreenEvent.GroupPlacesLimitReached -> {
                    showDialog = MainAlertDialog.PlanLimit(
                        content = MR.strings.plan_limit_places_alert_info.localizedString(),
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
            DefaultEmptyState(content = stringResource(MR.strings.error_load_data_generic))
        }

        is Payload.Success -> {
            PlacesScreen(
                state = state.data,
                userScrollEnabled = userScrollEnabled,
                onScreenClose = {
                    onNavigateBack()
                },
                onPlaceClick = {
                    openPlaceDetails(it)
                },
                onCreatePlace = {
                    vm.tryCreatePlace()
                },
                onErrorItemClick = {
                    askNotificationPermission()
                },
                mapContent = mapContent
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlacesScreen(
    state: PlacesListScreenState,
    userScrollEnabled: Boolean,
    onPlaceClick: (PlaceListItemData) -> Unit,
    onErrorItemClick: (ConditionErrorType) -> Unit,
    onScreenClose: () -> Unit,
    onCreatePlace: () -> Unit,
    mapContent: @Composable (PlacesListScreenState) -> Unit
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
                            painter = CoreAppIcons.Close.asPainterResource(),
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
                        Text(stringResource(MR.strings.places_list_add_button))
                    },
                    icon = {
                        Icon(
                            painter = CoreAppIcons.Add.asPainterResource(),
                            contentDescription = null
                        )
                    },
                    backgroundColor = MaterialTheme.colors.primary,
                    elevation = FloatingActionButtonDefaults.elevation(2.dp, 4.dp, 3.dp, 3.dp),
                    onClick = onCreatePlace
                )
            }
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
            ),
            userScrollEnabled = userScrollEnabled
        ) {
            if (state.places.isNotEmpty()) {

                this.item(key = "main_header") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsBigHeader(
                            name = stringResource(MR.strings.places_list_screen_title)
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
                            .padding(top = 8.dp),
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
                        content = stringResource(MR.strings.places_list_screen_empty_info),
                        icon = CoreAppIcons.MapsHomeWork.asPainterResource(),
                        buttonText = stringResource(MR.strings.places_create_button_create),
                        buttonIcon = CoreAppIcons.Add.asPainterResource()
                    ) {
                        onCreatePlace()
                    }
                }
            }
        }
    }
}
