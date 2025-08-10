package app.paralino.core.ui.places

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.Payload
import app.paralino.core.presentation.places.detail.PlaceDetailScreenState
import app.paralino.core.presentation.places.detail.PlaceDetailViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DataInfoChip
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.LargeSeekbarSwitch
import app.paralino.core.ui.components.LargeSeekbarSwitchState
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.dialogs.LoadingDialog
import app.paralino.core.ui.components.rememberSeekbarState
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun PlaceAlertsSettingsScreenCore(
    appState: CoreUiState,
    vm: PlaceDetailViewModel,
    contentPadding: PaddingValues,
) {

    MessageDispatcherEffect(vm, appState)
    val state by vm.uiState.collectAsState()

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
            PlaceAlertsScreen(
                state = finalState.data,
                paddingValues = contentPadding,
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
                }
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
private fun PlaceAlertsScreen(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    state: PlaceDetailScreenState,
    onTogglePlaceAlerts: () -> Unit,
    onTogglePlaceAlertsSeparately: () -> Unit,
    onEnterChange: (String) -> Unit,
    onExitChange: (String) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = paddingValues.calculateTopPadding(),
            bottom = paddingValues.calculateBottomPadding() + 40.dp,
        )
    ) {
        this.item(key = "main_header") {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                DataInfoChip(
                    modifier = Modifier,
                    image = state.image,
                    text = state.name
                )

                Spacer(modifier = Modifier.size(8.dp))

                val notificationState by rememberSeekbarState(
                    key = state,
                    state = LargeSeekbarSwitchState.SeekbarSwitchState(
                        seekbarState = null,
                        switchState = LargeSeekbarSwitchState.Switch(
                            checkState = state.arePlaceAlertsActive,
                            loadingState = state.isPlaceAlertsSwitchLoading
                        )
                    )
                )

                LargeSeekbarSwitch(
                    modifier = Modifier.fillMaxWidth(),
                    icon = if (state.arePlaceAlertsActive) {
                        CoreAppIcons.NotificationsActive.asPainterResource()
                    } else {
                        CoreAppIcons.NotificationsOff.asPainterResource()
                    },
                    title = stringResource(MR.strings.place_detail_notification_settings_title),
                    description = if (state.isPersonalSpace) {
                        stringResource(MR.strings.place_detail_notification_settings_info_personal)
                    } else {
                        stringResource(MR.strings.place_detail_notification_settings_info_group)
                    },
                    state = notificationState,
                    elevation = animateDpAsState(if (state.arePlaceAlertsActive) 16.dp else 0.dp).value,
                    onLongClick = onTogglePlaceAlertsSeparately,
                    onStateChange = {
                        onTogglePlaceAlerts()
                    }
                )
            }
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