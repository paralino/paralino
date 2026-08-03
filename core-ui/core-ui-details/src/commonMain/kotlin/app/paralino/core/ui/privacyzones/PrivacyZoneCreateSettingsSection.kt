package app.paralino.core.ui.privacyzones

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.presentation.privacyzones.create.CreatePrivacyZoneScreenState
import app.paralino.core.presentation.privacyzones.create.CreatePrivacyZoneScreenUiEvent
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.ConditionErrorItem
import app.paralino.core.ui.components.LargeSeekbarSwitch
import app.paralino.core.ui.components.LargeSeekbarSwitchState
import app.paralino.core.ui.components.rememberSeekbarState
import app.paralino.core.ui.groups.components.ComponentWithDurationExpirationBanner
import app.paralino.core.ui.share.getSharingTimeString
import app.paralino.core.ui.theme.AppColors
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun PrivacyZoneCreateSettingsSection(
    modifier: Modifier = Modifier,
    focusManager: FocusManager,
    state: CreatePrivacyZoneScreenState,
    onEvent: (CreatePrivacyZoneScreenUiEvent) -> Unit,
) {
    Column(modifier = modifier) {
        PrivacyZoneDurationSettings(
            modifier = Modifier.fillMaxWidth(),
            state = state,
            onDurationChanged = {
                onEvent(CreatePrivacyZoneScreenUiEvent.OnDurationChanged(it))
            },
            onExpirationEditRequested = {
                onEvent(CreatePrivacyZoneScreenUiEvent.OnExpirationEditRequested)
            },
        )

        Spacer(modifier = Modifier.size(10.dp))

        PrivacyZoneNotifyOthersSettings(
            modifier = Modifier.fillMaxWidth(),
            focusManager = focusManager,
            notifyOthersInZone = state.notifyOthersInZone,
            inZoneMessage = state.inZoneMessage,
            maxMessageLength = state.maxMessageLength,
            memberPreviewState = state.memberPreviewState,
            onNotifyOthersInZoneChanged = {
                onEvent(CreatePrivacyZoneScreenUiEvent.OnNotifyOthersInZoneChanged(it))
            },
            onInZoneMessageChanged = {
                onEvent(CreatePrivacyZoneScreenUiEvent.OnInZoneMessageChanged(it))
            },
        )
    }
}

@Composable
private fun PrivacyZoneDurationSettings(
    modifier: Modifier = Modifier,
    state: CreatePrivacyZoneScreenState,
    onDurationChanged: (Int) -> Unit,
    onExpirationEditRequested: () -> Unit,
) {
    Column(modifier = modifier) {
        AnimatedVisibility(visible = state.expirationStatus != null) {
            if (state.expirationStatus?.isExpired == true) {
                ConditionErrorItem(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = AppColors.errorBackground,
                    contentColor = AppColors.redMain,
                    icon = CoreAppIcons.Timer.asPainterResource(),
                    text = state.expirationStatus?.status.orEmpty(),
                    endAction = stringResource(MR.strings.button_activate),
                    endActionLoading = false,
                    onClick = onExpirationEditRequested
                )
            } else {
                ComponentWithDurationExpirationBanner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onExpirationEditRequested),
                    expirationTime = state.expirationStatus?.status.orEmpty(),
                    isExpired = false,
                    canEdit = true
                )
            }
        }

        AnimatedVisibility(state.showDurationSeekbar) {
            val durationState by rememberSeekbarState(
                key = state.durationMillis,
                state = LargeSeekbarSwitchState.SeekbarSwitchState(
                    seekbarState = LargeSeekbarSwitchState.Seekbar(
                        seekbarProgress = state.durationIndex + 1,
                        seekbarRange = IntRange(1, state.durationPresetsMillis.size)
                    ),
                    switchState = null
                )
            )

            LargeSeekbarSwitch(
                modifier = Modifier.fillMaxWidth(),
                icon = CoreAppIcons.AccessTime.asPainterResource(),
                title = stringResource(MR.strings.groups_sharing_section_title_duration),
                description = if (state.durationMillis <= 0) {
                    stringResource(MR.strings.privacy_zones_create_expiration_description_alwaysOn)
                } else {
                    stringResource(MR.strings.privacy_zones_create_expiration_description_expires)
                },
                bigTag = getSharingTimeString(state.durationMillis),
                isMinimumVerticalSpacing = false,
                focusDescription = false,
                state = durationState,
                onProgressChange = {
                    onDurationChanged(it - 1)
                },
            )
        }
    }
}
