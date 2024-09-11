package app.paralino.core.ui.places

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.Payload
import app.paralino.core.presentation.places.list.PlaceListItemData
import app.paralino.core.presentation.places.list.PlacesListScreenState
import app.paralino.core.presentation.places.list.PlacesScreenEvent
import app.paralino.core.presentation.places.list.PlacesViewModel
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.ConditionErrorItem
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DataInfoChip
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.PlanLimitCount
import app.paralino.core.ui.components.SmallActionButton
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.settings.PremiumPlanBannerSmall
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.extensions.errorString
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PlacesScreenCore(
    appState: CoreUiState,
    vm: PlacesViewModel,
    userScrollEnabled: Boolean,
    contentPadding: PaddingValues,
    askNotificationPermission: () -> Unit,
    addNewPlace: () -> Unit,
    openSubscriptions: () -> Unit,
    openPlaceDetails: (place: PlaceListItemData) -> Unit,
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
                contentPadding = contentPadding,
                onPlaceClick = {
                    openPlaceDetails(it)
                },
                onCreatePlace = {
                    vm.tryCreatePlace()
                },
                onPlanBannerClick = openSubscriptions,
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
    contentPadding: PaddingValues,
    onPlaceClick: (PlaceListItemData) -> Unit,
    onErrorItemClick: (ConditionErrorType) -> Unit,
    onCreatePlace: () -> Unit,
    onPlanBannerClick: () -> Unit,
    mapContent: @Composable (PlacesListScreenState) -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 20.dp,
                bottom = contentPadding.calculateBottomPadding() + 100.dp,
                start = 20.dp,
                end = 20.dp
            ),
            userScrollEnabled = userScrollEnabled
        ) {
            if (state.places.isNotEmpty()) {
                this.item(key = "main_header") {
                    DataInfoChip(
                        modifier = Modifier,
                        image = state.groupImage,
                        text = state.groupName
                    )
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
                    Spacer(modifier = Modifier.size(40.dp))
                }

                if (state.shouldShowPlanBanner) {
                    this.item(key = "plan_upgrade_banner") {
                        PremiumPlanBannerSmall(onClick = onPlanBannerClick)
                        Spacer(modifier = Modifier.size(40.dp))
                    }
                }

                this.stickyHeader(key = "count_header") {
                    Row(
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .background(MaterialTheme.colors.background)
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(MR.strings.places_list_screen_title),
                            style = MaterialTheme.typography.h6,
                            modifier = Modifier.alpha(ContentAlpha.medium)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        PlanLimitCount(
                            modifier = Modifier.padding(top = 4.dp),
                            count = state.places.size,
                            limit = if (state.canShowPlacesLimit) {
                                state.placesLimit
                            } else {
                                null
                            }
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        SmallActionButton(
                            modifier = Modifier,
                            icon = CoreAppIcons.Add.asPainterResource(),
                            text = {
                                Text(
                                    modifier = Modifier.padding(end = 4.dp),
                                    text = stringResource(MR.strings.group_details_add_member_button)
                                )
                            },
                            backgroundColor = MaterialTheme.colors.primary,
                            contentColor = MaterialTheme.colors.onPrimary,
                            onClick = onCreatePlace
                        )
                    }
                    Spacer(modifier = Modifier.size(4.dp))
                }
            }

            this.itemsIndexed(
                items = state.places,
                key = { index, item -> item.id }
            ) { index, item ->
                if (index > 0) {
                    Spacer(modifier = Modifier.size(8.dp))
                }
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
