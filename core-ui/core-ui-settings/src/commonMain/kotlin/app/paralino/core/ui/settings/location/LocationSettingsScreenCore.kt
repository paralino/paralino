package app.paralino.core.ui.settings.location

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.presentation.settings.location.LocationSettingsScreenEvent
import app.paralino.core.presentation.settings.location.LocationSettingsScreenUiEvent
import app.paralino.core.presentation.settings.location.LocationSettingsViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.LargeSeekbarSwitch
import app.paralino.core.ui.components.LargeSeekbarSwitchState
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.NoticeInfoChip
import app.paralino.core.ui.components.SettingsSectionHeader
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.components.rememberSeekbarState
import app.paralino.core.ui.components.GenericSettingsOptions
import app.paralino.core.utils.extensions.annotatedBoldCompose
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LocationSettingsScreenCore(
    coreUiState: CoreUiState,
    vm: LocationSettingsViewModel,
    paddingValues: PaddingValues,
    openPaywall: () -> Unit
) {
    MessageDispatcherEffect(vm, coreUiState)
    val uiState by vm.uiState.collectAsState()

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                LocationSettingsScreenEvent.OpenPaywall -> openPaywall()
                null -> {
                    /* do nothing */
                }
            }
        }
    }

    uiState.alertDialog?.let { dialog ->
        dialog.Show {
            vm.onEvent(
                LocationSettingsScreenUiEvent.DialogAction(
                    AlertDialogAction(
                        dialog = dialog,
                        action = it
                    )
                )
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = paddingValues
    ) {
        this.stickyHeader(key = "when_in_use") {
            SettingsSectionHeader(
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                name = stringResource(MR.strings.setting_location_highPerformance_section_title)
            )
        }

        this.item(key = "in_use_items") {
            Column(modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().animateItem()) {
                NoticeInfoChip(
                    modifier = Modifier,
                    icon = CoreAppIcons.LocationOn.asPainterResource(),
                    text = stringResource(MR.strings.setting_location_highPerformance_section_info)
                )

                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                    Text(
                        modifier = Modifier.padding(bottom = 10.dp, top = 20.dp),
                        text = stringResource(MR.strings.setting_location_highPerformance_duration_info).annotatedBoldCompose(),
                        style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
                        textAlign = TextAlign.Start
                    )
                }

                GenericSettingsOptions(
                    modifier = Modifier.fillMaxWidth(),
                    items = uiState.highPerformanceSettings.items,
                    onOptionSelected = {
                        vm.onEvent(
                            LocationSettingsScreenUiEvent.ChangePerformance(it)
                        )
                    }
                )
                Spacer(modifier = Modifier.size(20.dp))
            }
        }

        this.stickyHeader(key = "when_in_background") {
            SettingsSectionHeader(
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                name = stringResource(MR.strings.setting_location_balanced_section_title)
            )
        }

        this.item(key = "in_background_items") {
            Column(modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().animateItem()) {
                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                    Text(
                        modifier = Modifier.padding(bottom = 20.dp),
                        text = stringResource(MR.strings.setting_location_balanced_section_info).annotatedBoldCompose(),
                        style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
                        textAlign = TextAlign.Start
                    )
                }

                if (uiState.balancedSettings.showLocationIndicator != null) {
                    LargeSeekbarSwitch(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                        icon = CoreAppIcons.LocationOn.asPainterResource(),
                        title = stringResource(MR.strings.setting_location_balanced_locationIndicator_title),
                        description = stringResource(MR.strings.setting_location_balanced_locationIndicator_description),
                        state = rememberSeekbarState(
                            key = uiState,
                            state = LargeSeekbarSwitchState.SeekbarSwitchState(
                                seekbarState = null,
                                switchState = LargeSeekbarSwitchState.Switch(uiState.balancedSettings.showLocationIndicator ?: false)
                            )
                        ).value,
                        onStateChange = {
                            vm.onEvent(LocationSettingsScreenUiEvent.ToggleBalancedShowIndicator)
                        }
                    )
                }

                if (uiState.balancedSettings.pausesLocationAutomatically != null) {
                    LargeSeekbarSwitch(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                        icon = CoreAppIcons.StopCircle.asPainterResource(),
                        title = stringResource(MR.strings.setting_location_balanced_autoPause_title),
                        description = stringResource(MR.strings.setting_location_balanced_autoPause_description),
                        state = rememberSeekbarState(
                            key = uiState,
                            state = LargeSeekbarSwitchState.SeekbarSwitchState(
                                seekbarState = null,
                                switchState = LargeSeekbarSwitchState.Switch(uiState.balancedSettings.pausesLocationAutomatically ?: true)
                            )
                        ).value,
                        onStateChange = {
                            vm.onEvent(LocationSettingsScreenUiEvent.ToggleBalancedAutoPause)
                        }
                    )
                }

                LargeSeekbarSwitch(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    icon = CoreAppIcons.ShareLocation.asPainterResource(),
                    title = stringResource(MR.strings.setting_location_balanced_highAccuracy_title),
                    description = stringResource(MR.strings.setting_location_balanced_highAccuracy_description),
                    state = rememberSeekbarState(
                        key = uiState,
                        state = LargeSeekbarSwitchState.SeekbarSwitchState(
                            seekbarState = null,
                            switchState = LargeSeekbarSwitchState.Switch(uiState.balancedSettings.useHighPerformanceLocation)
                        )
                    ).value,
                    onStateChange = {
                        vm.onEvent(LocationSettingsScreenUiEvent.ToggleBalancedHighPerformance)
                    }
                )

                GenericSettingsOptions(
                    modifier = Modifier.fillMaxWidth(),
                    items = uiState.balancedSettings.items,
                    onOptionSelected = {
                        vm.onEvent(
                            LocationSettingsScreenUiEvent.ChangePerformance(it)
                        )
                    }
                )
                Spacer(modifier = Modifier.size(20.dp))
            }
        }

        this.stickyHeader(key = "battery_saving") {
            SettingsSectionHeader(
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                name = stringResource(MR.strings.setting_location_batterySaver_section_title)
            )
        }

        this.item(key = "battery_saving_items") {
            Column(modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().animateItem()) {
                NoticeInfoChip(
                    modifier = Modifier.padding(bottom = 20.dp),
                    icon = CoreAppIcons.BatteryFull.asPainterResource(),
                    text = stringResource(MR.strings.setting_location_batterySaver_batteryLow_info)
                )

                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                    Text(
                        modifier = Modifier.padding(bottom = 20.dp),
                        text = stringResource(MR.strings.setting_location_batterySaver_section_info).annotatedBoldCompose(),
                        style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
                        textAlign = TextAlign.Start
                    )
                }

                LargeSeekbarSwitch(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    icon = CoreAppIcons.Settings.asPainterResource(),
                    title = stringResource(MR.strings.setting_location_batterySaver_lowPowerMode_title),
                    description = stringResource(MR.strings.setting_location_batterySaver_lowPowerMode_description),
                    state = rememberSeekbarState(
                        key = uiState,
                        state = LargeSeekbarSwitchState.SeekbarSwitchState(
                            seekbarState = null,
                            switchState = LargeSeekbarSwitchState.Switch(uiState.batterySaverSettings.activeOnLowPowerMode)
                        )
                    ).value,
                    onStateChange = {
                        vm.onEvent(LocationSettingsScreenUiEvent.ToggleFollowDeviceLowPowerMode)
                    }
                )

                GenericSettingsOptions(
                    modifier = Modifier.fillMaxWidth(),
                    items = uiState.batterySaverSettings.items,
                    onOptionSelected = {
                        vm.onEvent(
                            LocationSettingsScreenUiEvent.ChangePerformance(it)
                        )
                    }
                )
                Spacer(modifier = Modifier.size(20.dp))
            }
        }

        if (false) {
            this.stickyHeader(key = "location_history") {
                SettingsSectionHeader(
                    modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                    name = stringResource(MR.strings.setting_location_history_section_title)
                )
            }

            this.item(key = "location_history_items") {
                Column(modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().animateItem()) {

                    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                        Text(
                            modifier = Modifier.padding(bottom = 20.dp),
                            text = stringResource(MR.strings.setting_location_history_section_info).annotatedBoldCompose(),
                            style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
                            textAlign = TextAlign.Start
                        )
                    }

                    GenericSettingsOptions(
                        modifier = Modifier.fillMaxWidth(),
                        items = uiState.historySettings.items,
                        onOptionSelected = {
                            vm.onEvent(
                                LocationSettingsScreenUiEvent.ChangePerformance(it)
                            )
                        }
                    )
                }
            }
        }

        this.item(key = "extra_space") {
            Spacer(modifier = Modifier.size(40.dp))
        }
    }
}