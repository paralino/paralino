package app.paralino.core.ui.onboarding.welcome

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.FloatingActionButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.LargeSeekbarSwitch
import app.paralino.core.ui.components.LargeSeekbarSwitchState
import app.paralino.core.ui.components.LargeSwitchDefaults
import app.paralino.core.ui.components.rememberSeekbarState
import app.paralino.core.ui.settings.location.SubscriptionOptionItem
import app.paralino.core.ui.share.getTime
import app.paralino.core.utils.SharingDurationPreset
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

@Composable
fun WelcomeOnboardingPageControl(
    modifier: Modifier = Modifier,
    animationStarted: Boolean
) {
    var animationProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        delay(10)
        animationProgress = 1f
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp)
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.surface)
            .padding(6.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        HighAccuracySwitch(
            modifier = Modifier
                .offset(
                    x = 30.dp,
                    y = animateDpAsState(
                        targetValue = animationProgress
                            .times(-maxHeight.value)
                            .plus(maxHeight.value + maxHeight.value * 0.2).dp,
                        animationSpec = tween(durationMillis = 300, delayMillis = 200)
                    ).value
                )
                .rotate(3f)
                .scale(0.8f)
        )

        BatterySwitch(
            modifier = Modifier
                .offset(
                    x = 10.dp,
                    y = animateDpAsState(
                        targetValue = animationProgress
                            .times(-maxHeight.value)
                            .plus(maxHeight.value).dp,
                        animationSpec = tween(durationMillis = 400, delayMillis = 100)
                    ).value
                )
                .rotate(-4f)
                .scale(0.9f)
        )

        SpeedSwitch(
            modifier = Modifier
                .offset(
                    x = maxWidth.div(2),
                    y = animateDpAsState(
                        targetValue = animationProgress
                            .times(-maxHeight.value)
                            .plus(maxHeight.value + maxHeight.value.times(0.02)).dp,
                        animationSpec = tween(durationMillis = 300, delayMillis = 100)
                    ).value
                )
                .rotate(8f)
                .scale(0.9f),
            animationStarted = animationStarted
        )

        BatteryPerformanceSwitch(
            modifier = Modifier
                .offset(
                    x = -30.dp,
                    y = animateDpAsState(
                        targetValue = animationProgress
                            .times(-maxHeight.value)
                            .plus(maxHeight.value + maxHeight.value * 0.42).dp,
                        animationSpec = tween(durationMillis = 400, delayMillis = 300)
                    ).value
                )
                .rotate(-1f)
                .scale(0.8f),
            animationStarted = animationStarted
        )

        BatteryPerformanceSwitch2(
            modifier = Modifier
                .offset(
                    x = 12.dp,
                    y = animateDpAsState(
                        targetValue = animationProgress
                            .times(-maxHeight.value)
                            .plus(maxHeight.value + maxHeight.value * 0.55).dp,
                        animationSpec = tween(durationMillis = 400, delayMillis = 400)
                    ).value
                )
                .rotate(-4f)
                .scale(0.9f)
        )

        DurationSwitch(
            modifier = Modifier
                .offset(
                    x = -20.dp,
                    y = animateDpAsState(
                        targetValue = animationProgress
                            .times(-maxHeight.value)
                            .plus(maxHeight.value + maxHeight.value * 0.7).dp,
                        animationSpec = tween(durationMillis = 500, delayMillis = 600)
                    ).value
                )
                .rotate(3f)
                .scale(0.8f)
        )

        ElevationSwitch(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(
                    x = maxWidth.times(0.05f),
                    y = animateDpAsState(
                        targetValue = animationProgress
                            .times(-maxHeight.value)
                            .plus(maxHeight.value + maxHeight.value.times(0.45f)).dp,
                        animationSpec = tween(durationMillis = 600, delayMillis = 500)
                    ).value
                )
                .rotate(5f)
                .scale(0.7f),
            animationStarted = animationStarted
        )

        SharingStopButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(
                    x = -maxWidth.times(0.02f),
                    y = animateDpAsState(
                        targetValue = animationProgress
                            .times(-maxHeight.value)
                            .plus(maxHeight.value + maxHeight.value.times(0.8f)).dp,
                        animationSpec = tween(durationMillis = 500, delayMillis = 800)
                    ).value
                )
                .rotate(-5f),
            animationStarted = animationStarted
        )
    }
}

@Composable
fun SpeedSwitch(modifier: Modifier = Modifier, animationStarted: Boolean) {
    var itemState by remember { mutableStateOf(true) }
    LaunchedEffect(animationStarted) {
        if (animationStarted) {
            delay(200)
            itemState = !itemState
        }
    }
    LargeSeekbarSwitch(
        modifier = modifier
            .height(LargeSwitchDefaults.minHeight)
            .fillMaxWidth(0.45f),
        icon = CoreAppIcons.Speed.asPainterResource(),
        title = stringResource(MR.strings.groups_sharing_setting_speed_share),
        description = null,
        state = rememberSeekbarState(
            key = itemState,
            state = LargeSeekbarSwitchState.SeekbarSwitchState(
                seekbarState = null,
                switchState = LargeSeekbarSwitchState.Switch(itemState)
            )
        ).value,
        onStateChange = {
            itemState = !itemState
        }
    )
}

@Composable
fun ElevationSwitch(modifier: Modifier = Modifier, animationStarted: Boolean) {
    var itemState by remember { mutableStateOf(true) }
    LaunchedEffect(animationStarted) {
        if (animationStarted) {
            delay(1200)
            itemState = !itemState
        }
    }
    LargeSeekbarSwitch(
        modifier = modifier
            .height(LargeSwitchDefaults.minHeight)
            .fillMaxWidth(0.45f),
        icon = CoreAppIcons.StackedLineChart.asPainterResource(),
        title = stringResource(MR.strings.groups_sharing_setting_elevation_share),
        description = null,
        state = rememberSeekbarState(
            key = itemState,
            state = LargeSeekbarSwitchState.SeekbarSwitchState(
                seekbarState = null,
                switchState = LargeSeekbarSwitchState.Switch(itemState)
            )
        ).value,
        onStateChange = {
            itemState = !itemState
        }
    )
}

@Composable
fun BatterySwitch(modifier: Modifier = Modifier) {
    var itemState by remember { mutableStateOf(true) }
    LargeSeekbarSwitch(
        modifier = modifier
            .height(LargeSwitchDefaults.minHeight)
            .fillMaxWidth(0.45f),
        icon = CoreAppIcons.BatteryChargingFull.asPainterResource(),
        title = stringResource(MR.strings.groups_sharing_setting_battery_share),
        description = null,
        state = rememberSeekbarState(
            key = itemState,
            state = LargeSeekbarSwitchState.SeekbarSwitchState(
                seekbarState = null,
                switchState = LargeSeekbarSwitchState.Switch(itemState)
            )
        ).value,
        onStateChange = {
            itemState = !itemState
        }
    )
}

@Composable
fun HighAccuracySwitch(modifier: Modifier = Modifier) {
    var itemState by remember { mutableStateOf(false) }
    LargeSeekbarSwitch(
        modifier = modifier
            .height(LargeSwitchDefaults.minHeight.times(1.2f))
            .fillMaxWidth(0.95f),
        icon = CoreAppIcons.ShareLocation.asPainterResource(),
        title = stringResource(MR.strings.setting_location_balanced_highAccuracy_title),
        description = stringResource(MR.strings.setting_location_balanced_highAccuracy_description),
        state = rememberSeekbarState(
            key = itemState,
            state = LargeSeekbarSwitchState.SeekbarSwitchState(
                seekbarState = null,
                switchState = LargeSeekbarSwitchState.Switch(itemState)
            )
        ).value,
        onStateChange = {
            itemState = !itemState
        }
    )
}

@Composable
fun BatteryPerformanceSwitch(modifier: Modifier = Modifier, animationStarted: Boolean) {
    var itemState by remember { mutableStateOf(false) }
    LaunchedEffect(animationStarted) {
        if (animationStarted) {
            delay(700)
            itemState = !itemState
        }
    }
    SubscriptionOptionItem(
        modifier = modifier.shadow(16.dp, RoundedCornerShape(10.dp)),
        title = stringResource(MR.strings.setting_location_balanced_level_5),
        description = stringResource(MR.strings.setting_location_balanced_level_5_info),
        batteryImpact = null,
        highBatteryImpact = true,
        isSelected = itemState,
        isPremiumFeatureEnabled = true,
        onClick = { itemState = !itemState }
    )
}

@Composable
fun BatteryPerformanceSwitch2(modifier: Modifier = Modifier) {
    var itemState by remember { mutableStateOf(false) }
    SubscriptionOptionItem(
        modifier = modifier.shadow(16.dp, RoundedCornerShape(10.dp)),
        title = stringResource(MR.strings.setting_location_balanced_level_1),
        description = stringResource(MR.strings.setting_location_balanced_level_1_info),
        batteryImpact = null,
        highBatteryImpact = false,
        isSelected = itemState,
        isPremiumFeatureEnabled = true,
        onClick = { itemState = !itemState }
    )
}

@Composable
fun SharingStopButton(modifier: Modifier = Modifier, animationStarted: Boolean) {
    var itemState by remember { mutableStateOf(false) }
    LaunchedEffect(animationStarted) {
        if (animationStarted) {
            delay(2000)
            itemState = !itemState
            delay(2000)
            itemState = !itemState
        }
    }
    if (itemState) {
        ExtendedFloatingActionButton(
            modifier = modifier,
            text = { Text(text = stringResource(MR.strings.location_banner_info_stop)) },
            icon = {
                Icon(
                    painter = CoreAppIcons.StopCircle.asPainterResource(),
                    contentDescription = null
                )
            },
            backgroundColor = MaterialTheme.colors.error,
            contentColor = MaterialTheme.colors.onError,
            elevation = FloatingActionButtonDefaults.elevation(
                2.dp, 4.dp, 3.dp, 3.dp
            ),
            onClick = { itemState = false }
        )
    } else {
        ExtendedFloatingActionButton(
            modifier = modifier,
            text = { Text(text = stringResource(MR.strings.groups_sharing_button_start)) },
            icon = {
                Icon(
                    painter = CoreAppIcons.ShareLocation.asPainterResource(),
                    contentDescription = null
                )
            },
            backgroundColor = MaterialTheme.colors.primary,
            elevation = FloatingActionButtonDefaults.elevation(
                2.dp, 4.dp, 3.dp, 3.dp
            ),
            onClick = { itemState = true }
        )
    }
}

@Composable
fun DurationSwitch(modifier: Modifier = Modifier) {
    var itemState by remember {
        mutableLongStateOf(8.hours.inWholeMilliseconds)
    }

    fun sharingTimeIndex(sharingTimeMillis: Long): Int {
        return SharingDurationPreset.presets.indexOfFirst { it == sharingTimeMillis }
            .takeIf { it >= 0 }
            ?: SharingDurationPreset.presets.indexOfFirst { sharingTimeMillis < it }
                .takeIf { it >= 0 }
            ?: SharingDurationPreset.presets.lastIndex
    }

    LargeSeekbarSwitch(
        modifier = modifier
            .height(LargeSwitchDefaults.minHeight)
            .fillMaxWidth(0.95f),
        state = rememberSeekbarState(
            key = itemState,
            state = LargeSeekbarSwitchState.SeekbarSwitchState(
                seekbarState = LargeSeekbarSwitchState.Seekbar(
                    seekbarProgress = sharingTimeIndex(itemState) + 1,
                    seekbarRange = IntRange(1, SharingDurationPreset.presets.size)
                ),
                switchState = null
            )
        ).value,
        icon = CoreAppIcons.AccessTime.asPainterResource(),
        title = stringResource(MR.strings.groups_sharing_section_title_duration),
        description = getTime(itemState),
        focusDescription = true,
        onProgressChange = {
            itemState =
                SharingDurationPreset.presets.getOrNull(it - 1) ?: 1.days.inWholeMilliseconds
        }
    )
}