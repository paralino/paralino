package app.paralino.core.ui.places

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.dp
import app.paralino.core.resources.moko.*
import app.paralino.core.models.Payload
import app.paralino.core.models.PlaceZoneExpirationStatus
import app.paralino.core.models.PlaceZoneType
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.presentation.common.PlaceListItemData
import app.paralino.core.presentation.places.detail.PlaceDetailScreenEvent
import app.paralino.core.presentation.places.detail.PlaceDetailScreenState
import app.paralino.core.presentation.places.detail.PlaceDetailViewModel
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.EditImageIcon
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.SettingsActionItem
import app.paralino.core.ui.components.SmallActionButton
import app.paralino.core.ui.components.dialogs.LoadingDialog
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.errorString
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PlaceDetailScreenCore(
    appState: CoreUiState,
    vm: PlaceDetailViewModel,
    userScrollEnabled: Boolean,
    contentPadding: PaddingValues,
    onNavigateBack: () -> Unit,
    openPlaceRawDataScreen: () -> Unit,
    openPlaceAlertsScreen: () -> Unit,
    openPlaceImprovementsScreen: () -> Unit,
    openPlaceNameEditScreen: () -> Unit,
    openPlacePositionEditScreen: () -> Unit,
    mapContent: @Composable (PlaceDetailScreenState) -> Unit,
) {

    MessageDispatcherEffect(vm, appState)
    val state by vm.uiState.collectAsState()
    var showDialog by remember { mutableStateOf<MainAlertDialog?>(null) }

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                is PlaceDetailScreenEvent.Error -> {
                    vm.showSnackbar(event.error.errorString())
                }

                is PlaceDetailScreenEvent.PlaceDeleted -> {
                    onNavigateBack()
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

    when (val finalState = state) {
        Payload.Loading -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(),
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
            PlaceDetailScreen(
                state = finalState.data,
                paddingValues = contentPadding,
                canEditPlace = finalState.data.canEdit,
                canHaveAlerts = finalState.data.canHaveAlerts,
                userScrollEnabled = userScrollEnabled,
                openPlaceNameEditScreen = openPlaceNameEditScreen,
                openPlacePositionEditScreen = openPlacePositionEditScreen,
                openPlaceRawDataScreen = openPlaceRawDataScreen,
                openPlaceAlertsScreen = openPlaceAlertsScreen,
                openPlaceImprovementsScreen = openPlaceImprovementsScreen,
                onPlaceDelete = {
                    showDialog = MainAlertDialog.DeletePlace
                },
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

@Composable
private fun PlaceDetailScreen(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    state: PlaceDetailScreenState,
    canEditPlace: Boolean,
    canHaveAlerts: Boolean,
    userScrollEnabled: Boolean,
    openPlaceRawDataScreen: () -> Unit,
    openPlaceAlertsScreen: () -> Unit,
    openPlaceImprovementsScreen: () -> Unit,
    openPlaceNameEditScreen: () -> Unit,
    openPlacePositionEditScreen: () -> Unit,
    onPlaceDelete: () -> Unit,
    mapContent: @Composable (PlaceDetailScreenState) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = paddingValues.calculateTopPadding(),
            bottom = paddingValues.calculateBottomPadding() + 40.dp,
        ),
        userScrollEnabled = userScrollEnabled
    ) {
        this.item(key = "main_header") {
            Row(
                modifier = Modifier
                    .padding(top = 20.dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlaceListItem(
                    modifier = Modifier.weight(1f),
                    state = remember(state) {
                        PlaceListItemData(
                            id = state.placeId,
                            groupOrSpaceId = state.groupOrSpaceId,
                            type = PlaceZoneType.PlaceWithAlert,
                            name = state.name,
                            image = state.image,
                            expirationStatus = PlaceZoneExpirationStatus.ActiveAlways(showStatus = false),
                            private = state.private,
                            lat = 0.0,
                            lng = 0.0,
                            radiusMeters = 0
                        )
                    },
                    hasEndArrow = false,
                    backgroundColor = Color.Transparent
                )

                if (state.canEdit) {
                    EditImageIcon(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .clickable(onClick = openPlaceNameEditScreen)
                    )
                }
            }
        }

        this.item(key = "map") {
            // map
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .padding(horizontal = 20.dp)
                    .height(240.dp)
                    .background(
                        color = MaterialTheme.colors.background,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(
                        border = BorderStroke(6.dp, MaterialTheme.colors.background),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .dropShadow(
                        shape = RoundedCornerShape(20.dp),
                        shadow = Shadow(
                            radius = 12.dp,
                            color = MaterialTheme.colors.onBackground.copy(alpha = 0.2f),
                        )
                    )
                    .clip(RoundedCornerShape(20.dp)),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(5.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colors.background,
                ) {
                    mapContent(state)
                }

                if (canEditPlace) {
                    SmallActionButton(
                        modifier = Modifier
                            .padding(12.dp)
                            .align(Alignment.BottomEnd)
                            .dropShadow(
                                shape = CircleShape,
                                shadow = Shadow(
                                    radius = 16.dp,
                                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.2f),
                                )
                            ),
                        icon = CoreAppIcons.Map.asPainterResource(),
                        text = {
                            Text(
                                modifier = Modifier.padding(end = 2.dp)
                                    .weight(1f, fill = false),
                                text = stringResource(MR.strings.place_detail_change_location)
                            )
                        },
                        backgroundColor = MaterialTheme.colors.primary,
                        contentColor = MaterialTheme.colors.onPrimary,
                        isLoading = false,
                        hasEndArrow = true,
                        onClick = openPlacePositionEditScreen
                    )
                }
            }
        }

        this.item(key = "settings") {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(modifier = Modifier.size(20.dp))
                SettingsActionItem(
                    name = stringResource(MR.strings.raw_data_screen_title),
                    icon = CoreAppIcons.Security.asPainterResource(),
                ) {
                    openPlaceRawDataScreen()
                }

                if (canHaveAlerts) {
                    SettingsActionItem(
                        name = stringResource(MR.strings.place_detail_notification_settings_title),
                        icon = CoreAppIcons.NotificationsActive.asPainterResource(),
                    ) {
                        openPlaceAlertsScreen()
                    }
                    SettingsActionItem(
                        name = stringResource(MR.strings.place_improvements_screen_title),
                        icon = CoreAppIcons.AutoFixHigh.asPainterResource(),
                    ) {
                        openPlaceImprovementsScreen()
                    }
                }

                if (canEditPlace) {
                    SettingsActionItem(
                        name = stringResource(MR.strings.place_list_menu_delete),
                        icon = CoreAppIcons.Delete.asPainterResource(),
                        iconBackgroundTint = AppColors.errorBackground
                    ) {
                        onPlaceDelete()
                    }
                }
                Spacer(modifier = Modifier.size(20.dp))
            }
        }
    }
}