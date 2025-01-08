package app.paralino.core.ui.settings.location

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.models.base.DeviceType
import app.paralino.core.presentation.settings.location.LocationSettingsItemState
import app.paralino.core.presentation.settings.location.LocationSettingsScreenEvent
import app.paralino.core.presentation.settings.location.LocationSettingsScreenUiEvent
import app.paralino.core.presentation.settings.location.LocationSettingsViewModel
import app.paralino.core.provider.current
import app.paralino.core.repo.location.LocationPerformanceSettingsType
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.LargeSeekbarSwitch
import app.paralino.core.ui.components.LargeSeekbarSwitchState
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.NoticeInfoChip
import app.paralino.core.ui.components.SettingsSectionHeader
import app.paralino.core.ui.components.SmallActionButton
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.components.rememberSeekbarState
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.annotatedBold
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
                        text = stringResource(MR.strings.setting_location_highPerformance_duration_info).annotatedBold(),
                        style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
                        textAlign = TextAlign.Start
                    )
                }

                LocationPerformanceSettingsOptions(
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
                        text = stringResource(MR.strings.setting_location_balanced_section_info).annotatedBold(),
                        style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
                        textAlign = TextAlign.Start
                    )
                }

                val highAccuracyState by rememberSeekbarState(
                    key = uiState,
                    LargeSeekbarSwitchState.SeekbarSwitchState(
                        seekbarState = null,
                        switchState = LargeSeekbarSwitchState.Switch(uiState.balancedSettings.useHighPerformanceLocation)
                    )
                )

                LargeSeekbarSwitch(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    icon = CoreAppIcons.ShareLocation.asPainterResource(),
                    title = stringResource(MR.strings.setting_location_balanced_highAccuracy_title),
                    description = stringResource(MR.strings.setting_location_balanced_highAccuracy_description),
                    state = highAccuracyState,
                    onStateChange = {
                        vm.onEvent(LocationSettingsScreenUiEvent.ToggleBalancedHighPerformance)
                    }
                )

                LocationPerformanceSettingsOptions(
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
                        modifier = Modifier,
                        text = stringResource(MR.strings.setting_location_batterySaver_section_info).annotatedBold(),
                        style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
                        textAlign = TextAlign.Start
                    )
                }

                LocationPerformanceSettingsOptions(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    items = uiState.batterySaverSettings.items,
                    onOptionSelected = {
                        vm.onEvent(
                            LocationSettingsScreenUiEvent.ChangePerformance(it)
                        )
                    }
                )
            }
        }

        this.item(key = "extra_space") {
            Spacer(modifier = Modifier.size(40.dp))
        }
    }
}

@Composable
private fun LocationPerformanceSettingsOptions(
    modifier: Modifier = Modifier,
    items: List<LocationSettingsItemState>,
    onOptionSelected: (LocationPerformanceSettingsType) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color = MaterialTheme.colors.surface)
    ) {
        items.forEachIndexed { index, state ->
            if (index > 0) {
                Divider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = Color.Gray.copy(alpha = 0.2f),
                    thickness = 0.5.dp
                )
            }

            SubscriptionOptionItem(
                title = state.title,
                description = state.description,
                batteryImpact = state.batteryImpact,
                highBatteryImpact = state.highBatteryImpact,
                isSelected = state.isSelected,
                isPremiumFeatureEnabled = state.isPremiumFeatureEnabled,
                onClick = { onOptionSelected(state.performance) }
            )
        }
    }
}

@Composable
fun SubscriptionOptionItem(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    batteryImpact: String?,
    highBatteryImpact: Boolean,
    isSelected: Boolean,
    isPremiumFeatureEnabled: Boolean,
    onClick: () -> Unit
) {
    val sourceColor = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.surface
    val backgroundColor by animateColorAsState(targetValue = sourceColor)
    val contentColor by animateColorAsState(targetValue = contentColorFor(backgroundColor = sourceColor))
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(2.dp)
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelected) {
            Icon(
                modifier = Modifier
                    .align(Alignment.Top)
                    .size(24.dp)
                    .background(
                        MaterialTheme.colors.onPrimary,
                        shape = CircleShape
                    )
                    .padding(1.dp),
                painter = CoreAppIcons.CheckCircle.asPainterResource(),
                contentDescription = null,
                tint = MaterialTheme.colors.primary
            )
        } else {
            Icon(
                modifier = Modifier
                    .align(Alignment.Top)
                    .size(24.dp)
                    .background(backgroundColor, shape = CircleShape)
                    .padding(2.dp),
                painter = CoreAppIcons.RadioButtonUnchecked.asPainterResource(),
                contentDescription = null,
                tint = contentColor.copy(alpha = ContentAlpha.disabled)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .alpha(if (isPremiumFeatureEnabled) 1f else 0.5f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.SemiBold,
                lineHeight = if (DeviceType.current == DeviceType.iOS) 22.sp else 24.sp,
                color = contentColor
            )

            if (batteryImpact != null) {
                SmallActionButton(
                    icon = null,
                    backgroundColor = if (highBatteryImpact) {
                        MaterialTheme.colors.error
                    } else {
                        AppColors.accent.copy(alpha = 0.4f)
                    },
                    contentColor = if (highBatteryImpact) {
                        MaterialTheme.colors.onError
                    } else {
                        animateColorAsState(
                            if (isSelected) {
                                AppColors.lightBackground.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colors.onBackground.copy(alpha = 0.7f)
                            }
                        ).value
                    },
                    text = {
                        Text(
                            text = batteryImpact,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    compactMode = true,
                    onClick = null
                )
            }

            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                Text(
                    modifier = Modifier,
                    text = description.annotatedBold(),
                    style = MaterialTheme.typography.caption,
                    textAlign = TextAlign.Start,
                    color = contentColor
                )
            }
        }

        if (!isPremiumFeatureEnabled) {
            Icon(
                painter = CoreAppIcons.StarFamily.asPainterResource(),
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
                modifier = Modifier
                    .align(Alignment.Top)
                    .padding(start = 8.dp)
                    .size(24.dp)
                    .background(
                        color = AppColors.highlightGold,
                        shape = CircleShape
                    )
                    .padding(4.dp)
            )
        }
    }
}