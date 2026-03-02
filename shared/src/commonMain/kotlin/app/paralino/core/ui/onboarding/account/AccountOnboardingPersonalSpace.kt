package app.paralino.core.ui.onboarding.account

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.PushProvider
import app.paralino.core.models.location.MemberDeviceData
import app.paralino.core.models.location.MemberDeviceOnlineStatusType
import app.paralino.core.models.location.MemberDeviceStatusType
import app.paralino.core.models.location.UserDeviceModel
import app.paralino.core.models.user.UserSession
import app.paralino.core.presentation.groups.GroupMemberItemLocationState
import app.paralino.core.presentation.groups.MemberLocationVisibilityType
import app.paralino.core.presentation.personal_space.UserDeviceAction
import app.paralino.core.presentation.personal_space.UserDeviceItemState
import app.paralino.core.presentation.personal_space.UserDeviceOrSessionItemState
import app.paralino.core.presentation.personal_space.UserSessionItemState
import app.paralino.core.provider.NetworkSignalStrength
import app.paralino.core.provider.NetworkType
import app.paralino.core.provider.current
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.personal_space.UserDeviceItem
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.nowUTC
import app.paralino.core.utils.extensions.pulsate
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random

@Composable
fun AccountOnboardingPersonalSpace(
    modifier: Modifier = Modifier
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
            .padding(bottom = 20.dp, top = 12.dp)
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.surface)
            .padding(6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.accentDark.copy(alpha = 0.2f)),
        contentAlignment = Alignment.TopCenter
    ) {
        DeviceContent1(
            modifier = Modifier
                .offset(
                    y = animateDpAsState(
                        targetValue = animationProgress
                            .times(-maxHeight.value)
                            .plus(maxHeight.value + maxHeight.value * 0.05).dp,
                        animationSpec = tween(durationMillis = 400, delayMillis = 100)
                    ).value
                )
                .rotate(3f)
        )

        DeviceContent2(
            modifier = Modifier
                .offset(
                    y = animateDpAsState(
                        targetValue = animationProgress
                            .times(-maxHeight.value)
                            .plus(maxHeight.value + maxHeight.value * 0.38).dp,
                        animationSpec = tween(durationMillis = 500, delayMillis = 300)
                    ).value
                )
                .rotate(-3f)
        )

        DeviceContent3(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(
                    y = animateDpAsState(
                        targetValue = animationProgress
                            .times(-maxHeight.value)
                            .plus(maxHeight.value - maxHeight.value * 0.05).dp,
                        animationSpec = tween(durationMillis = 600, delayMillis = 500)
                    ).value
                )
                .rotate(2f)
        )
    }
}

@Composable
private fun DeviceContent1(modifier: Modifier = Modifier) {
    val data = remember {
        createDummyDeviceWithSession(
            deviceName = "",
            isCurrentDevice = true,
            hasLocation = true,
            batteryLevel = 87,
            isCharging = false,
            onlineStatus = MemberDeviceOnlineStatusType.ONLINE,
            deviceStatus = MemberDeviceStatusType.ACTIVE,
            networkType = NetworkType.G5,
            signalStrength = NetworkSignalStrength.GREAT,
            deviceType = DeviceType.current,
            time = MR.strings.group_members_last_active_status_now.localizedString()
        )
    }

    UserDeviceItem(
        modifier = modifier
            .fillMaxWidth(0.95f)
            .pulsate(pulseAmount = 0.9f),
        elevation = 8.dp,
        state = data,
        onEvent = { }
    )
}

@Composable
private fun DeviceContent2(modifier: Modifier = Modifier) {
    val data = remember {
        createDummyDeviceWithSession(
            deviceName = "Galaxy Tab S10",
            isCurrentDevice = false,
            hasLocation = true,
            batteryLevel = 23,
            isCharging = true,
            onlineStatus = MemberDeviceOnlineStatusType.BUSY,
            deviceStatus = MemberDeviceStatusType.IDLE,
            networkType = NetworkType.WIFI,
            signalStrength = NetworkSignalStrength.GOOD,
            deviceType = DeviceType.Android,
            time = MR.strings.group_members_last_active_status_recently_2.localizedString()
        )
    }

    UserDeviceItem(
        modifier = modifier
            .fillMaxWidth(0.95f)
            .pulsate(pulseAmount = 0.9f)
            .clickable { },
        elevation = 8.dp,
        state = data,
        onEvent = { }
    )
}

@Composable
private fun DeviceContent3(modifier: Modifier = Modifier) {
    val data = remember {
        createDummyDeviceWithSession(
            deviceName = "iPhone 15",
            isCurrentDevice = false,
            hasLocation = false,
            batteryLevel = null,
            isCharging = null,
            onlineStatus = MemberDeviceOnlineStatusType.OFFLINE,
            deviceStatus = MemberDeviceStatusType.BATTERY_SAVER_ON,
            networkType = NetworkType.G4,
            signalStrength = NetworkSignalStrength.GOOD,
            deviceType = DeviceType.iOS,
            time = ""
        )
    }

    UserDeviceItem(
        modifier = modifier
            .fillMaxWidth(0.95f)
            .pulsate(pulseAmount = 0.9f)
            .clickable { },
        elevation = 8.dp,
        state = data,
        onEvent = { }
    )
}

private fun createDummyDeviceWithSession(
    deviceName: String,
    isCurrentDevice: Boolean,
    hasLocation: Boolean,
    batteryLevel: Int?,
    isCharging: Boolean?,
    onlineStatus: MemberDeviceOnlineStatusType,
    deviceStatus: MemberDeviceStatusType,
    networkType: NetworkType,
    signalStrength: NetworkSignalStrength,
    deviceType: DeviceType,
    time: String
): UserDeviceOrSessionItemState.DeviceWithSession {
    val deviceId = "device_${deviceName.lowercase().replace(" ", "_")}"
    val sessionId = "session_${deviceId}"

    val userSession = UserSession(
        id = sessionId,
        createdAt = nowUTC().toLocalDateTime(TimeZone.UTC).toString(),
        updatedAt = nowUTC().toLocalDateTime(TimeZone.UTC).toString(),
        expire = nowUTC().toLocalDateTime(TimeZone.UTC).toString(),
        provider = "email",
        ip = "192.168.1.100",
        osCode = "ios",
        osName = "iOS",
        osVersion = "17.2",
        clientType = "mobile",
        clientCode = "ios",
        clientName = "Paralino iOS",
        clientVersion = "1.0.0",
        clientEngine = "WebKit",
        clientEngineVersion = "17.2",
        deviceName = deviceName,
        deviceBrand = if (deviceName.contains("iPhone")) "Apple" else if (deviceName.contains("Samsung")) "Samsung" else "Apple",
        deviceModel = deviceName,
        countryCode = "US",
        countryName = "United States",
        current = isCurrentDevice
    )

    val userDevice = UserDeviceModel(
        id = deviceId,
        sessionId = sessionId,
        deviceType = deviceType,
        ownerUserId = "user_123",
        deviceId = deviceId,
        userKeyVersion = 1L,
        location = null,
        deviceData = CryptoResource.Ready(
            MemberDeviceData(
                deviceName = deviceName,
                deviceType = "mobile"
            )
        ),
        batteryAlertSettings = null,
        unifiedPushConfig = null,
        pushProvider = PushProvider.DEVICE_NATIVE
    )

    val deviceItemState = UserDeviceItemState(
        device = userDevice,
        location = createDummyLocationState(
            hasLocation = hasLocation,
            deviceId = deviceId,
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            onlineStatus = onlineStatus,
            deviceStatus = deviceStatus,
            signalStrength = signalStrength,
            networkType = networkType,
            time = time,
        ),
        deleteAction = if (!hasLocation) UserDeviceAction.DeleteDevice() else null,
        deactivateAction = if (hasLocation) UserDeviceAction.DeactivateDevice() else null,
        isCurrentDevice = isCurrentDevice
    )

    val sessionItemState = UserSessionItemState(
        session = userSession
    )

    return UserDeviceOrSessionItemState.DeviceWithSession(
        device = deviceItemState,
        session = sessionItemState
    )
}


private fun createDummyLocationState(
    hasLocation: Boolean,
    deviceId: String,
    batteryLevel: Int?,
    isCharging: Boolean?,
    onlineStatus: MemberDeviceOnlineStatusType,
    deviceStatus: MemberDeviceStatusType,
    networkType: NetworkType,
    signalStrength: NetworkSignalStrength,
    time: String,
): GroupMemberItemLocationState {
    return GroupMemberItemLocationState(
        locationId = deviceId,
        deviceId = deviceId,
        name = "Device 1",
        image = ImageModel.emoji(Emojis.lock),
        lat = null,
        lng = null,
        actualBatteryLevel = batteryLevel,
        adjustedBatteryLevel = batteryLevel,
        isBatteryCharging = isCharging ?: false,
        speed = if (hasLocation && networkType != NetworkType.WIFI) "${Random.nextInt(20, 40)} km/h" else null,
        elevation = if (hasLocation) "${Random.nextInt(10, 40).times(10)}m" else null,
        lastLocationPing = if (hasLocation) time else null,
        lastStatusPing = time,
        accuracyMeters = if (hasLocation) 400f else null,
        deviceStatus = deviceStatus,
        canEdit = true,
        currentDevice = true,
        onlineStatus = onlineStatus,
        networkType = networkType,
        signalStrength = signalStrength,
        visibility = MemberLocationVisibilityType.VISIBLE_ALWAYS
    )
}
