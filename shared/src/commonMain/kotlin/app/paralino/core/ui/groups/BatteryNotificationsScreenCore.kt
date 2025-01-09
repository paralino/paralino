package app.paralino.core.ui.groups

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.Resource
import app.paralino.core.presentation.groups.settings.BatteryNotificationScreenEvent
import app.paralino.core.presentation.groups.settings.BatteryNotificationScreenUiEvent
import app.paralino.core.presentation.groups.settings.BatteryNotificationsViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DataInfoChip
import app.paralino.core.ui.components.LargeSeekbarSwitch
import app.paralino.core.ui.components.LargeSeekbarSwitchState
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.NoticeInfoChip
import app.paralino.core.ui.components.rememberSeekbarState
import app.paralino.core.ui.groups.components.BatteryNotificationItem
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest

@Composable
fun BatteryNotificationsScreenCore(
    appState: CoreUiState,
    vm: BatteryNotificationsViewModel,
    contentPadding: PaddingValues,
    openPaywall: () -> Unit,
) {
    MessageDispatcherEffect(vm, appState)
    val state by vm.uiState.collectAsState()


    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                null -> {
                    /* do nothing */
                }

                BatteryNotificationScreenEvent.OpenPaywall -> openPaywall()
            }
        }
    }

    when (val finalState = state) {
        Resource.NotInitialized -> {
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

        is Resource.Ready -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding
            ) {
                this.item(key = "group_header") {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        DataInfoChip(
                            modifier = Modifier,
                            image = finalState.data.groupImage,
                            text = finalState.data.groupName
                        )

                        Spacer(modifier = Modifier.size(20.dp))

                        NoticeInfoChip(
                            modifier = Modifier,
                            icon = CoreAppIcons.BatteryFull.asPainterResource(),
                            text = stringResource(MR.strings.group_settings_battery_notifications_notice)
                        )

                        Spacer(modifier = Modifier.size(20.dp))

                        val notificationState by rememberSeekbarState(
                            key = finalState,
                            LargeSeekbarSwitchState.SeekbarSwitchState(
                                seekbarState = null,
                                switchState = LargeSeekbarSwitchState.Switch(
                                    checkState = finalState.data.isActive,
                                    loadingState = finalState.data.isActiveSwitchLoading
                                )
                            )
                        )

                        LargeSeekbarSwitch(
                            modifier = Modifier.fillMaxWidth(),
                            icon = if (finalState.data.isActive) {
                                CoreAppIcons.NotificationsActive.asPainterResource()
                            } else {
                                CoreAppIcons.NotificationsOff.asPainterResource()
                            },
                            title = stringResource(MR.strings.group_settings_battery_notifications_title),
                            description = stringResource(MR.strings.group_settings_battery_notifications_info),
                            state = notificationState,
                            elevation = animateDpAsState(if (finalState.data.isActive) 16.dp else 0.dp).value,
                            onStateChange = {
                                vm.onEvent(BatteryNotificationScreenUiEvent.ToggleBatterySettingForGroup)
                            }
                        )
                    }
                }

                this.itemsIndexed(
                    items = finalState.data.settings,
                    key = { index, item -> item.memberUserId }
                ) { index, item ->
                    BatteryNotificationItem(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .alpha(if (finalState.data.isActive) 1f else ContentAlpha.disabled)
                            .animateItem(),
                        state = item,
                        onNotifyChange = {
                            vm.onEvent(
                                BatteryNotificationScreenUiEvent.ToggleBatterySettingForUser(
                                    item.memberUserId
                                )
                            )
                        }
                    )
                }

                this.item(key = "extra_space") {
                    Spacer(modifier = Modifier.size(40.dp))
                }
            }
        }
    }
}