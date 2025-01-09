package app.paralino.core.ui.places

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.Payload
import app.paralino.core.presentation.places.detail.PlaceDetailScreenEvent
import app.paralino.core.presentation.places.detail.PlaceDetailScreenState
import app.paralino.core.presentation.places.detail.PlaceDetailViewModel
import app.paralino.core.presentation.places.list.PlaceListItemData
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.LargeSeekbarSwitch
import app.paralino.core.ui.components.LargeSeekbarSwitchState
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.SettingsSectionHeader
import app.paralino.core.ui.components.dialogs.LoadingDialog
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.components.rememberSeekbarState
import app.paralino.core.ui.settings.SettingsActionItem
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
    openPlaceEditScreen: () -> Unit,
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
                userScrollEnabled = userScrollEnabled,
                openPlaceEditScreen = openPlaceEditScreen,
                openPlaceRawDataScreen = openPlaceRawDataScreen,
                onPlaceDelete = {
                    showDialog = MainAlertDialog.DeletePlace
                },
                onTogglePlaceAlerts = {
                    vm.togglePlaceAlertsSettings(revokeItemState = false)
                },
                onTogglePlaceAlertsSeparately = {
                    vm.togglePlaceAlertsSettings(revokeItemState = true)
                },
                onEnterChange = {
                    vm.toggleEnterSettings(it)
                },
                onExitChange = {
                    vm.toggleExitSettings(it)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaceDetailScreen(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    state: PlaceDetailScreenState,
    canEditPlace: Boolean,
    userScrollEnabled: Boolean,
    openPlaceRawDataScreen: () -> Unit,
    openPlaceEditScreen: () -> Unit,
    onPlaceDelete: () -> Unit,
    onTogglePlaceAlerts: () -> Unit,
    onTogglePlaceAlertsSeparately: () -> Unit,
    onEnterChange: (String) -> Unit,
    onExitChange: (String) -> Unit,
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
            PlaceListItem(
                modifier = Modifier.padding(top = 26.dp).padding(horizontal = 20.dp),
                state = PlaceListItemData(
                    id = state.placeId,
                    groupId = state.groupId,
                    name = state.name,
                    image = state.image,
                    private = state.private,
                    lat = 0.0,
                    lng = 0.0,
                    radiusMeters = 0
                ),
                hasEndArrow = false,
                backgroundColor = Color.Transparent
            )
        }

        this.item(key = "map") {
            // map
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(top = 26.dp)
                    .padding(horizontal = 20.dp),
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

        this.item(key = "settings") {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(modifier = Modifier.size(20.dp))
                SettingsActionItem(
                    name = stringResource(MR.strings.raw_data_screen_title),
                    icon = CoreAppIcons.Security.asPainterResource(),
                ) {
                    openPlaceRawDataScreen()
                }
                if (canEditPlace) {
                    SettingsActionItem(
                        name = stringResource(MR.strings.places_edit_screen_title),
                        icon = CoreAppIcons.Edit.asPainterResource(),
                    ) {
                        openPlaceEditScreen()
                    }
                    SettingsActionItem(
                        name = stringResource(MR.strings.place_list_menu_delete),
                        icon = CoreAppIcons.Delete.asPainterResource(),
                        iconTint = AppColors.errorBackground
                    ) {
                        onPlaceDelete()
                    }
                }
                Spacer(modifier = Modifier.size(20.dp))
            }
        }

        if (state.settings.isNotEmpty()) {
            this.stickyHeader(key = "notification_header") {
                SettingsSectionHeader(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    name = stringResource(MR.strings.place_detail_notification_settings_title)
                )
            }

            this.item(key = "notification_notice") {
                Spacer(modifier = Modifier.size(4.dp))
                val notificationState by rememberSeekbarState(
                    key = state,
                    LargeSeekbarSwitchState.SeekbarSwitchState(
                        seekbarState = null,
                        switchState = LargeSeekbarSwitchState.Switch(
                            checkState = state.arePlaceAlertsActive,
                            loadingState = state.isPlaceAlertsSwitchLoading
                        )
                    )
                )

                LargeSeekbarSwitch(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    icon = if (state.arePlaceAlertsActive) {
                        CoreAppIcons.NotificationsActive.asPainterResource()
                    } else {
                        CoreAppIcons.NotificationsOff.asPainterResource()
                    },
                    title = stringResource(MR.strings.place_detail_notification_settings_title),
                    description = stringResource(MR.strings.place_detail_notification_settings_info),
                    state = notificationState,
                    elevation = animateDpAsState(if (state.arePlaceAlertsActive) 16.dp else 0.dp).value,
                    onLongClick = onTogglePlaceAlertsSeparately,
                    onStateChange = {
                        onTogglePlaceAlerts()
                    }
                )
                Spacer(modifier = Modifier.size(20.dp))
            }

            this.itemsIndexed(
                items = state.settings,
                key = { index, item -> item.memberUserId }
            ) { index, item ->
                PlaceDetailUserListItem(
                    modifier = Modifier
                        .animateItem()
                        .alpha(if (state.arePlaceAlertsActive) 1f else ContentAlpha.disabled)
                        .padding(horizontal = 20.dp),
                    state = item,
                    onEnterChange = onEnterChange,
                    onExitChange = onExitChange,
                )
            }
        }
    }
}