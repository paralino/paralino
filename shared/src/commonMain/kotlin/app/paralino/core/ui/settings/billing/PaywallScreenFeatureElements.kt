package app.paralino.core.ui.settings.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.location.MemberDeviceOnlineStatusType
import app.paralino.core.models.location.MemberDeviceStatusType
import app.paralino.core.presentation.groups.GroupMemberItemEvent
import app.paralino.core.presentation.groups.GroupMemberItemLocationState
import app.paralino.core.presentation.groups.GroupMemberItemStateWithLocations
import app.paralino.core.presentation.groups.GroupMemberItemStateWithOneLocation
import app.paralino.core.presentation.groups.LocationRequestItemState
import app.paralino.core.presentation.groups.MemberActionsState
import app.paralino.core.presentation.groups.MemberLocationVisibilityType
import app.paralino.core.provider.DistanceType
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.NetworkSignalStrength
import app.paralino.core.provider.NetworkType
import app.paralino.core.provider.localizedString
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.LargeSeekbarSwitch
import app.paralino.core.ui.components.LargeSeekbarSwitchState
import app.paralino.core.ui.components.rememberSeekbarState
import app.paralino.core.ui.groups.components.GroupMember
import app.paralino.core.ui.groups.components.dummyMemberState
import app.paralino.core.ui.onboarding.welcome.HistoryMapOnboardingContent
import app.paralino.core.ui.settings.GenericSettingsOptionItem
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.nowUTC
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.minutes

@Composable
fun PaywallScreenFeatureElements(
    modifier: Modifier,
    memberImageModel: ImageModel,
    memberName: String,
    memberDeviceName: String,
    displayTitle: Boolean,
    displayLocationHistory: Boolean,
    valueFormatter: IValueFormatter
) {
    Column(
        modifier = modifier
            .padding(horizontal = 20.dp)
    ) {

        if (displayTitle) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                text = stringResource(MR.strings.paywall_header_try_it),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold)
            )
        }

        Icon(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(24.dp)
                .padding(2.dp)
                .rotate(90f),
            painter = CoreAppIcons.ChevronRight.asPainterResource(),
            contentDescription = null
        )

        if (displayLocationHistory) {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .height(400.dp)
                    .rotate(2f)
                    .shadow(8.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colors.surface)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                HistoryMapOnboardingContent(
                    modifier = Modifier.fillMaxSize(),
                    animate = false,
                )
            }
        }

        MemberItem(
            modifier = Modifier
                .padding(top = 20.dp)
                .rotate(-2f),
            memberName = memberName,
            memberImageModel = memberImageModel,
            memberDeviceName = memberDeviceName,
            valueFormatter = valueFormatter
        )

        var itemState1 by remember { mutableStateOf(true) }
        var itemState2 by remember { mutableStateOf(false) }

        BatteryPerformance1Switch(
            modifier = Modifier
                .rotate(2f),
            itemState = itemState1,
            onClick = {
                itemState1 = true
                itemState2 = false
            }
        )

        BatteryPerformance2Switch(
            modifier = Modifier
                .rotate(-3f),
            itemState = itemState2,
            onClick = {
                itemState1 = false
                itemState2 = true
            }
        )

        NotifyMembersSwitch(
            modifier = Modifier
                .padding(top = 8.dp)
                .rotate(2f),
        )

        MemberItemWithLocations(
            modifier = Modifier
                .padding(top = 8.dp)
                .rotate(-2f),
            memberName = memberName,
            memberImageModel = memberImageModel,
            memberDeviceName = memberDeviceName,
            valueFormatter = valueFormatter
        )
    }
}

@Composable
private fun MemberItem(
    memberImageModel: ImageModel,
    memberName: String,
    memberDeviceName: String,
    modifier: Modifier = Modifier,
    valueFormatter: IValueFormatter
) {
    val uiScope = rememberCoroutineScope()

    var location by remember {
        mutableStateOf(
            GroupMemberItemLocationState(
                locationId = "",
                deviceId = "",
                name = memberDeviceName,
                image = ImageModel.emoji(Emojis.lock),
                lat = null,
                lng = null,
                actualBatteryLevel = 58,
                adjustedBatteryLevel = 58,
                isBatteryCharging = false,
                speed = valueFormatter.getSpeedSmall(metersPerSecond = 1.4f),
                elevation = valueFormatter.getDistanceSmall(meters = 250.0, distanceType = DistanceType.ELEVATION),
                lastLocationPing = valueFormatter.formatRelativeDateTime(
                    nowUTC().minus(30.minutes).toLocalDateTime(TimeZone.UTC)
                ),
                lastStatusPing = valueFormatter.formatRelativeDateTime(
                    nowUTC().minus(30.minutes).toLocalDateTime(TimeZone.UTC)
                ),
                accuracyMeters = 50f,
                deviceStatus = MemberDeviceStatusType.IDLE,
                canEdit = false,
                currentDevice = true,
                onlineStatus = MemberDeviceOnlineStatusType.BUSY,
                networkType = NetworkType.G2,
                signalStrength = NetworkSignalStrength.POOR,
                visibility = MemberLocationVisibilityType.VISIBLE_ALWAYS
            )
        )
    }

    var memberActions: MemberActionsState by remember {
        mutableStateOf(
            MemberActionsState(
                locationRequest = LocationRequestItemState(
                    isEnabled = true,
                    lastLocationRequest = null,
                    lastDeviceUpdate = nowUTC().minus(20.minutes),
                    isPremiumFeatureEnabled = true
                )
            )
        )
    }

    GroupMember(
        modifier = modifier,
        state = GroupMemberItemStateWithOneLocation(
            memberState = dummyMemberState.copy(
                name = memberName, image = memberImageModel
            ),
            singleLocation = location,
            deviceId = "",
            actions = memberActions,
            isHighlighted = true,
            isCollapsedOrMinimized = false
        ),
        forceExpandAction = true,
        collapseOnActionClick = false,
        backgroundColor = MaterialTheme.colors.background,
        canShowOldLocation = true
    ) {
        when (it) {
            is GroupMemberItemEvent.onLocationRequest -> {
                uiScope.launch {
                    memberActions = memberActions.copy(
                        locationRequest = memberActions.locationRequest?.copy(
                            isLoading = true,
                            isWaitingForUser = false
                        )
                    )
                    delay(1000)
                    memberActions = memberActions.copy(
                        locationRequest = memberActions.locationRequest?.copy(
                            isLoading = true,
                            isWaitingForUser = true
                        )
                    )
                    delay(2500)
                    memberActions = memberActions.copy(locationRequest = null)
                    location = location.copy(
                        accuracyMeters = 40f,
                        actualBatteryLevel = 42,
                        adjustedBatteryLevel = 42,
                        lastStatusPing = MR.strings.group_members_last_active_status_now.localizedString(),
                        deviceStatus = MemberDeviceStatusType.ACTIVE_NO_LOCATION,
                        onlineStatus = MemberDeviceOnlineStatusType.ONLINE
                    )
                    delay(1500)
                    location = location.copy(
                        accuracyMeters = 20f,
                        deviceStatus = MemberDeviceStatusType.ACTIVE,
                        onlineStatus = MemberDeviceOnlineStatusType.ONLINE,
                        networkType = NetworkType.G4,
                        signalStrength = NetworkSignalStrength.GREAT,
                        lastLocationPing = MR.strings.group_members_last_active_status_now.localizedString(),
                        speed = valueFormatter.getSpeedSmall(metersPerSecond = 1.2f),
                        elevation = valueFormatter.getDistanceSmall(meters = 180.0, distanceType = DistanceType.ELEVATION),
                    )
                }
            }

            else -> {}
        }
    }
}

@Composable
private fun MemberItemWithLocations(
    memberImageModel: ImageModel,
    memberName: String,
    memberDeviceName: String,
    modifier: Modifier = Modifier,
    valueFormatter: IValueFormatter
) {
    val uiScope = rememberCoroutineScope()

    val locations = remember {
        listOf(
            GroupMemberItemLocationState(
                locationId = "",
                deviceId = "",
                name = memberDeviceName,
                image = ImageModel.emoji(Emojis.lock),
                lat = null,
                lng = null,
                actualBatteryLevel = 58,
                adjustedBatteryLevel = 58,
                isBatteryCharging = false,
                speed = valueFormatter.getSpeedSmall(metersPerSecond = 1.4f),
                elevation = valueFormatter.getDistanceSmall(meters = 250.0, distanceType = DistanceType.ELEVATION),
                lastLocationPing = valueFormatter.formatRelativeDateTime(
                    nowUTC().toLocalDateTime(TimeZone.UTC)
                ),
                lastStatusPing = valueFormatter.formatRelativeDateTime(
                    nowUTC().minus(30.minutes).toLocalDateTime(TimeZone.UTC)
                ),
                accuracyMeters = 200f,
                deviceStatus = MemberDeviceStatusType.ACTIVE,
                canEdit = false,
                currentDevice = true,
                onlineStatus = MemberDeviceOnlineStatusType.ONLINE,
                networkType = NetworkType.G2,
                signalStrength = NetworkSignalStrength.POOR,
                visibility = MemberLocationVisibilityType.VISIBLE_ALWAYS
            ),
            GroupMemberItemLocationState(
                locationId = "",
                deviceId = "",
                name = "$memberDeviceName #2",
                image = ImageModel.emoji(Emojis.lock),
                lat = null,
                lng = null,
                actualBatteryLevel = 75,
                adjustedBatteryLevel = 75,
                isBatteryCharging = true,
                speed = valueFormatter.getSpeedSmall(metersPerSecond = 1.3f),
                elevation = valueFormatter.getDistanceSmall(meters = 280.0, distanceType = DistanceType.ELEVATION),
                lastLocationPing = valueFormatter.formatRelativeDateTime(
                    nowUTC().minus(20.minutes).toLocalDateTime(TimeZone.UTC)
                ),
                lastStatusPing = valueFormatter.formatRelativeDateTime(
                    nowUTC().minus(20.minutes).toLocalDateTime(TimeZone.UTC)
                ),
                accuracyMeters = 100f,
                deviceStatus = MemberDeviceStatusType.IDLE,
                canEdit = false,
                currentDevice = false,
                onlineStatus = MemberDeviceOnlineStatusType.BUSY,
                networkType = NetworkType.G4,
                signalStrength = NetworkSignalStrength.GREAT,
                visibility = MemberLocationVisibilityType.VISIBLE_ALWAYS
            )
        )
    }

    val memberActions: MemberActionsState = remember {
        MemberActionsState()
    }

    GroupMember(
        modifier = modifier,
        state = GroupMemberItemStateWithLocations(
            memberState = dummyMemberState.copy(
                name = memberName, image = memberImageModel
            ),
            locations = locations,
            actions = memberActions,
            allowShowingDevices = true,
            lastLocationPing = locations.first().lastLocationPing,
            lastStatusPing = locations.first().lastStatusPing
        ),
        forceExpandAction = true,
        collapseOnActionClick = false,
        backgroundColor = MaterialTheme.colors.background,
        canShowOldLocation = false
    ) {

    }
}

@Composable
private fun BatteryPerformance1Switch(
    modifier: Modifier = Modifier,
    itemState: Boolean,
    onClick: () -> Unit
) {
    GenericSettingsOptionItem(
        modifier = modifier.shadow(16.dp, RoundedCornerShape(10.dp)),
        title = stringResource(MR.strings.setting_location_balanced_level_4),
        description = stringResource(MR.strings.setting_location_balanced_level_4_info),
        batteryImpact = null,
        highBatteryImpact = true,
        isSelected = itemState,
        isPremiumFeatureEnabled = true,
        isLoading = false,
        onClick = onClick
    )
}

@Composable
private fun BatteryPerformance2Switch(
    modifier: Modifier = Modifier,
    itemState: Boolean,
    onClick: () -> Unit
) {
    GenericSettingsOptionItem(
        modifier = modifier.shadow(16.dp, RoundedCornerShape(10.dp)),
        title = stringResource(MR.strings.setting_location_balanced_level_5),
        description = stringResource(MR.strings.setting_location_balanced_level_5_info),
        batteryImpact = null,
        highBatteryImpact = true,
        isSelected = itemState,
        isPremiumFeatureEnabled = true,
        isLoading = false,
        onClick = onClick
    )
}

@Composable
private fun NotifyMembersSwitch(modifier: Modifier = Modifier) {
    var itemState by remember { mutableStateOf(true) }
    LargeSeekbarSwitch(
        modifier = modifier.fillMaxWidth(),
        icon = CoreAppIcons.NotificationsActive.asPainterResource(),
        title = stringResource(MR.strings.groups_sharing_setting_notifyOthers_title),
        description = stringResource(MR.strings.groups_sharing_setting_notifyOthers_description),
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