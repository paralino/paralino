package app.paralino.core.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import app.paralino.core.resources.moko.*
import app.paralino.core.models.base.ParalinoSocketConnectionType
import app.paralino.core.models.base.PushServiceConnectionState
import app.paralino.core.models.location.LocationProviderType
import app.paralino.core.presentation.common.endActionTextRes
import app.paralino.core.presentation.common.nameRes
import app.paralino.core.presentation.common.resolvedNameRes
import app.paralino.core.presentation.settings.status.AppStatusAllowanceState
import app.paralino.core.presentation.settings.status.AppStatusApiTestState
import app.paralino.core.presentation.settings.status.AppStatusConnectionState
import app.paralino.core.presentation.settings.status.AppStatusCurrentLocationState
import app.paralino.core.presentation.settings.status.AppStatusLocationModeState
import app.paralino.core.presentation.settings.status.AppStatusPushServiceState
import app.paralino.core.presentation.settings.status.AppStatusRequirementState
import app.paralino.core.presentation.settings.status.AppStatusScreenUiEvent
import app.paralino.core.presentation.settings.status.AppStatusSharingState
import app.paralino.core.presentation.settings.status.AppStatusViewModel
import app.paralino.core.provider.ConnectionStatus
import app.paralino.core.provider.NetworkSignalStrength
import app.paralino.core.provider.NetworkType
import app.paralino.core.provider.localizedString
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.ConditionErrorItem
import app.paralino.core.ui.components.DataInfoChip
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.SettingsSectionHeader
import app.paralino.core.ui.components.icon
import app.paralino.core.ui.groups.components.NetworkTypeIcon
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.extensions.annotatedBoldCompose
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun AppStatusScreenCore(
    appState: CoreUiState,
    vm: AppStatusViewModel,
    paddingValues: PaddingValues,
    askNotificationPermission: () -> Unit,
    askLocationService: () -> Unit,
    askLocationPermission: () -> Unit,
    askBackgroundLocationPermission: () -> Unit,
    askBackgroundAccessPermission: () -> Unit,
    askPushNotificationService: () -> Unit,
    openLocationSettings: () -> Unit,
    openDebugLogs: () -> Unit,
) {

    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = paddingValues.calculateBottomPadding() + 20.dp)
    ) {
        SettingsSectionHeader(name = stringResource(MR.strings.setting_status_section_requirements))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            uiState.requirements.forEach { requirement ->
                AppStatusRequirementItem(
                    requirement = requirement,
                    onClick = {
                        when (requirement.type) {
                            ConditionErrorType.GPS -> askLocationService()
                            is ConditionErrorType.PermissionLocation -> askLocationPermission()
                            ConditionErrorType.PermissionLocationBackground -> askBackgroundLocationPermission()
                            ConditionErrorType.PermissionNotifications -> askNotificationPermission()
                            ConditionErrorType.UnrestrictedBackgroundAccess -> askBackgroundAccessPermission()
                            is ConditionErrorType.PushNotificationService -> askPushNotificationService()
                            ConditionErrorType.NewAppUpdate -> vm.onEvent(AppStatusScreenUiEvent.TriggerAppUpdate)
                            ConditionErrorType.Network -> vm.showSnackbar(
                                text = MR.strings.error_no_connection_snackbar.localizedString()
                            )

                            else -> {}
                        }
                    }
                )
            }
        }

        uiState.connection?.let { connection ->
            SettingsSectionHeader(name = stringResource(MR.strings.setting_status_section_connection))
            AppStatusConnectionTable(
                connection = connection,
                apiTest = uiState.apiTest,
                onTestApi = { vm.onEvent(AppStatusScreenUiEvent.TestApiConnection) },
                onClearApiTest = { vm.onEvent(AppStatusScreenUiEvent.ClearApiTestResult) }
            )
        }

        uiState.pushService?.let { pushService ->
            SettingsSectionHeader(name = stringResource(MR.strings.setting_notificationSocket_title))
            AppStatusPushServiceTable(pushService = pushService)
        }

        uiState.locationMode?.let { locationMode ->
            SettingsSectionHeader(name = stringResource(MR.strings.setting_status_section_locationMode))
            AppStatusLocationMode(
                locationMode = locationMode,
                onBoost = { isLong -> vm.onEvent(AppStatusScreenUiEvent.BoostLocation(isLong = isLong)) },
                onStopBoost = { vm.onEvent(AppStatusScreenUiEvent.StopBoost) },
                onIncreaseTime = openLocationSettings
            )
        }

        SettingsSectionHeader(name = stringResource(MR.strings.setting_status_section_sharing))
        AppStatusSharing(sharing = uiState.sharing)

        SettingsSectionHeader(name = stringResource(MR.strings.setting_status_section_currentLocation))
        val currentLocation = uiState.currentLocation
        if (currentLocation != null) {
            AppStatusCurrentLocation(location = currentLocation)
        } else {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                Text(
                    text = stringResource(MR.strings.setting_status_location_none),
                    style = MaterialTheme.typography.body2
                )
            }
        }

        if (uiState.isDevMode) {
            Spacer(modifier = Modifier.size(16.dp))
            AppStatusTable {
                AppStatusTableRow(
                    label = "Debug logs",
                    labelColor = AppColors.accentDark,
                    showDivider = false,
                    onClick = openDebugLogs
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = CoreAppIcons.ChevronRight.asPainterResource(),
                        tint = AppColors.accentDark,
                        contentDescription = null
                    )
                }
            }
        }

        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 4.dp, end = 4.dp),
                text = stringResource(MR.strings.setting_status_refreshInfo),
                style = MaterialTheme.typography.caption,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AppStatusRequirementItem(
    requirement: AppStatusRequirementState,
    onClick: () -> Unit
) {
    val type = requirement.type
    val resolvedNameRes = type.resolvedNameRes.takeIf { requirement.isResolved }
    val isResolved = resolvedNameRes != null

    ConditionErrorItem(
        backgroundColor = if (isResolved) AppColors.accent.copy(alpha = 0.15f) else AppColors.errorBackground,
        contentColor = if (isResolved) AppColors.accentDark else AppColors.redMain,
        icon = if (isResolved) CoreAppIcons.CheckCircle.asPainterResource() else type.icon,
        text = stringResource(resolvedNameRes ?: type.nameRes),
        endAction = if (isResolved) null else type.endActionTextRes?.let { stringResource(it) },
        onClick = if (isResolved) null else onClick
    )
}

@Composable
private fun AppStatusConnectionTable(
    connection: AppStatusConnectionState,
    apiTest: AppStatusApiTestState,
    onTestApi: () -> Unit,
    onClearApiTest: () -> Unit
) {
    AppStatusTable {
        AppStatusTableRow(
            label = stringResource(MR.strings.setting_status_connection_status),
            value = stringResource(
                when (connection.status) {
                    ConnectionStatus.CONNECTED -> MR.strings.setting_status_connection_online
                    ConnectionStatus.CONNECTED_NO_INTERNET -> MR.strings.setting_status_connection_noInternet
                    ConnectionStatus.OFFLINE -> MR.strings.setting_status_connection_offline
                }
            ),
            valueColor = if (connection.status.isOnline) AppColors.accentDark else AppColors.redMain,
            showDivider = false
        )
        connection.networkType?.let { networkType ->
            AppStatusTableRow(
                label = stringResource(MR.strings.setting_status_connection_network),
                value = when (networkType) {
                    NetworkType.WIFI -> networkType.networkName
                    NetworkType.GENERIC_CELL -> stringResource(MR.strings.setting_status_connection_cellular)
                    NetworkType.G2,
                    NetworkType.G3,
                    NetworkType.G4,
                    NetworkType.G5 -> "${stringResource(MR.strings.setting_status_connection_cellular)} · ${networkType.networkName}"
                },
                valueColor = if (networkType == NetworkType.G2) AppColors.redMain else Color.Unspecified
            )
            connection.signalStrength?.let { signalStrength ->
                AppStatusTableRow(
                    label = stringResource(MR.strings.setting_status_connection_signal),
                    value = stringResource(
                        when (signalStrength) {
                            NetworkSignalStrength.POOR -> MR.strings.setting_status_signal_poor
                            NetworkSignalStrength.GOOD -> MR.strings.setting_status_signal_good
                            NetworkSignalStrength.GREAT -> MR.strings.setting_status_signal_great
                        }
                    ),
                    valueColor = if (signalStrength == NetworkSignalStrength.POOR) AppColors.redMain else Color.Unspecified,
                    leadingValue = {
                        NetworkTypeIcon(networkType = networkType, signalStrength = signalStrength)
                    }
                )
            }
        }
        if (connection.status.isOnline) {
            AppStatusApiTestRow(
                apiTest = apiTest,
                onTestApi = onTestApi,
                onClearApiTest = onClearApiTest
            )
        }
    }
}

@Composable
private fun AppStatusPushServiceTable(pushService: AppStatusPushServiceState) {
    val connectionState = pushService.connectionState
    val connectionType = pushService.connectionType
    AppStatusTable {
        AppStatusTableRow(
            label = stringResource(MR.strings.setting_status_connection_status),
            value = stringResource(
                when (connectionState) {
                    PushServiceConnectionState.Connected -> MR.strings.setting_status_pushService_connected
                    PushServiceConnectionState.Connecting -> MR.strings.setting_status_pushService_connecting
                    PushServiceConnectionState.Scheduled -> MR.strings.setting_status_pushService_scheduled
                    PushServiceConnectionState.Disconnected -> MR.strings.setting_status_pushService_disconnected
                    null -> MR.strings.setting_status_pushService_notRunning
                }
            ),
            valueColor = when (connectionState) {
                PushServiceConnectionState.Connected -> AppColors.accentDark
                PushServiceConnectionState.Connecting,
                PushServiceConnectionState.Scheduled -> Color.Unspecified
                PushServiceConnectionState.Disconnected,
                null -> AppColors.redMain
            },
            showDivider = false
        )
        AppStatusTableRow(
            label = stringResource(MR.strings.setting_status_pushService_mode),
            value = stringResource(
                when (connectionType) {
                    ParalinoSocketConnectionType.REALTIME -> MR.strings.setting_notificationSocket_connectionType_realtime_title
                    ParalinoSocketConnectionType.PERIODIC_LEVEL_1 -> MR.strings.setting_notificationSocket_connectionType_periodic1_title
                    ParalinoSocketConnectionType.PERIODIC_LEVEL_2 -> MR.strings.setting_notificationSocket_connectionType_periodic2_title
                    ParalinoSocketConnectionType.PERIODIC_LEVEL_3 -> MR.strings.setting_notificationSocket_connectionType_periodic3_title
                }
            ),
            valueColor = if (connectionType != ParalinoSocketConnectionType.REALTIME) AppColors.redMain else Color.Unspecified
        )
    }
}

@Composable
private fun AppStatusApiTestRow(
    apiTest: AppStatusApiTestState,
    onTestApi: () -> Unit,
    onClearApiTest: () -> Unit
) {
    AppStatusTableRow(
        label = stringResource(MR.strings.setting_status_connection_api),
        onClick = if (apiTest is AppStatusApiTestState.Failed) onClearApiTest else null
    ) {
        when (apiTest) {
            AppStatusApiTestState.Idle -> {
                AppStatusTableButton(
                    text = stringResource(MR.strings.setting_status_connection_apiTest),
                    onClick = onTestApi
                )
            }

            AppStatusApiTestState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colors.onSurface,
                    strokeWidth = 2.dp
                )
            }

            AppStatusApiTestState.Success -> {
                AppStatusTableValue(
                    modifier = Modifier.weight(1f),
                    value = stringResource(MR.strings.setting_status_connection_apiOk),
                    valueColor = AppColors.accentDark
                )
            }

            is AppStatusApiTestState.Failed -> {
                AppStatusTableValue(
                    modifier = Modifier.weight(1f),
                    value = apiTest.failedTags.joinToString(", "),
                    valueColor = AppColors.redMain
                )
            }
        }
    }
}

@Composable
private fun AppStatusLocationMode(
    locationMode: AppStatusLocationModeState,
    onBoost: (isLong: Boolean) -> Unit,
    onStopBoost: () -> Unit,
    onIncreaseTime: () -> Unit
) {
    AppStatusTable {
        AppStatusTableRow(
            label = stringResource(MR.strings.setting_status_locationMode_current),
            value = locationMode.current,
            valueColor = if (locationMode.isThrottled) AppColors.redMain else Color.Unspecified,
            showDivider = false
        )
        locationMode.timeLeft?.let { timeLeft ->
            AppStatusTableRow(
                label = stringResource(MR.strings.setting_status_locationMode_timeLeft),
                value = timeLeft
            )
        }
        if (locationMode.isThrottled) {
            locationMode.requested?.let { requested ->
                AppStatusTableRow(
                    label = stringResource(MR.strings.setting_status_locationMode_requested),
                    value = requested
                )
            }
        }
        if (locationMode.isThrottled || locationMode.isManualBoost || locationMode.isDevMode) {
            AppStatusTableRow(label = stringResource(MR.strings.setting_status_locationMode_boost)) {
                if (locationMode.isManualBoost) {
                    AppStatusTableButton(
                        text = stringResource(MR.strings.setting_status_locationMode_stopTest),
                        onClick = onStopBoost
                    )
                } else {
                    AppStatusTableButton(
                        text = stringResource(MR.strings.setting_status_connection_apiTest),
                        onClick = { onBoost(false) },
                        onLongClick = if (locationMode.isDevMode) {
                            { onBoost(true) }
                        } else {
                            null
                        }
                    )
                }
            }
        }
        if (locationMode.isThrottled) {
            AppStatusTableRow(
                label = stringResource(MR.strings.setting_status_locationMode_increaseTime),
                labelColor = AppColors.accentDark,
                onClick = onIncreaseTime
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = CoreAppIcons.ChevronRight.asPainterResource(),
                    tint = AppColors.accentDark,
                    contentDescription = null
                )
            }
        }
    }

    if (locationMode.allowances.isNotEmpty()) {
        Column(
            modifier = Modifier
                .padding(top = 14.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colors.surface)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            locationMode.allowances.forEachIndexed { index, state ->
                if (index > 0) {
                    Divider(
                        modifier = Modifier,
                        color = Color.Gray.copy(alpha = 0.2f),
                        thickness = 0.5.dp
                    )
                }

                AppStatusAllowance(allowance = state)
            }
        }
    }
}

@Composable
private fun AppStatusAllowance(allowance: AppStatusAllowanceState) {
    val color = if (allowance.isUsedUp) AppColors.redMain else AppColors.accent
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f),
                text = allowance.name,
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.SemiBold
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = allowance.time ?: stringResource(MR.strings.setting_status_value_missing),
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold,
                    color = if (allowance.isUsedUp) AppColors.redMain else Color.Unspecified
                )
                if (allowance.time != null) {
                    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                        Text(
                            text = stringResource(MR.strings.setting_status_locationMode_window, allowance.windowMinutes.toString()),
                            style = MaterialTheme.typography.caption
                        )
                    }
                }
            }
        }
        allowance.slots?.let { slots ->
            AppStatusAllowanceSlots(slots = slots, color = color)
        }
    }
}

@Composable
private fun AppStatusAllowanceSlots(slots: List<Float>, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        slots.forEach { usedFraction ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colors.onSurface.copy(alpha = 0.1f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                if (usedFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(usedFraction)
                            .background(color)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppStatusSharing(sharing: AppStatusSharingState) {
    if (sharing.groups.isEmpty() && !sharing.isSharingToPersonalSpace) {
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
                text = stringResource(MR.strings.setting_status_sharing_none),
                style = MaterialTheme.typography.body2
            )
        }
        return
    }

    val personalSpaceName = stringResource(MR.strings.personalSpace_spaceTitle)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        sharing.groups.forEach { group ->
            DataInfoChip(
                image = group.image,
                text = group.name
            )
        }
        DataInfoChip(
            modifier = Modifier.alpha(if (sharing.isSharingToPersonalSpace) 1f else ContentAlpha.medium),
            image = sharing.personalSpaceImage,
            text = if (sharing.isSharingToPersonalSpace) {
                personalSpaceName
            } else {
                stringResource(MR.strings.setting_status_sharing_off, personalSpaceName)
            }
        )
    }
}

@Composable
private fun AppStatusCurrentLocation(location: AppStatusCurrentLocationState) {
    Column(
        modifier = Modifier.padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = CoreAppIcons.LocationOn.asPainterResource(),
                tint = AppColors.accentDark,
                contentDescription = null
            )
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = location.coordinates,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.SemiBold
            )
        }
        location.time?.let { time ->
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = CoreAppIcons.AccessTime.asPainterResource(),
                        contentDescription = null
                    )
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = time,
                        style = MaterialTheme.typography.body2
                    )
                }
            }
        }
    }

    AppStatusTable {
        AppStatusTableRow(
            label = stringResource(MR.strings.setting_status_location_accuracy),
            value = stringResource(MR.strings.setting_status_location_accuracyValue, location.accuracy),
            valueColor = if (location.isAccuracyLow) AppColors.redMain else Color.Unspecified,
            showDivider = false
        )
        val missingValue = stringResource(MR.strings.setting_status_value_missing)
        AppStatusTableRow(
            label = stringResource(MR.strings.setting_status_location_speed),
            value = location.speed ?: missingValue,
            valueColor = if (location.speed == null) AppColors.redMain else Color.Unspecified
        )
        AppStatusTableRow(
            label = stringResource(MR.strings.setting_status_location_elevation),
            value = location.elevation ?: missingValue,
            valueColor = if (location.elevation == null) AppColors.redMain else Color.Unspecified
        )
        AppStatusTableRow(
            label = stringResource(MR.strings.setting_status_location_source),
            value = when (location.provider) {
                LocationProviderType.GPS -> stringResource(MR.strings.setting_status_location_source_gps)
                LocationProviderType.NETWORK -> stringResource(MR.strings.setting_status_location_source_network)
                LocationProviderType.FUSED -> stringResource(MR.strings.setting_status_location_source_fused)
                null -> missingValue
            },
            valueColor = if (location.provider == null) AppColors.redMain else Color.Unspecified
        )
        AppStatusTableRow(
            label = stringResource(MR.strings.setting_status_location_filtered),
            value = stringResource(if (location.isFiltered) MR.strings.button_yes else MR.strings.button_no)
        )
    }
}

@Composable
private fun AppStatusTable(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colors.surface)
    ) {
        content()
    }
}

@Composable
private fun AppStatusTableRow(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified,
    showDivider: Boolean = true,
    leadingValue: (@Composable () -> Unit)? = null
) {
    AppStatusTableRow(
        label = label,
        showDivider = showDivider
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            leadingValue?.invoke()
            AppStatusTableValue(value = value, valueColor = valueColor)
        }
    }
}

@Composable
private fun AppStatusTableRow(
    label: String,
    labelColor: Color = Color.Unspecified,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    value: @Composable RowScope.() -> Unit
) {
    if (showDivider) {
        Divider(
            modifier = Modifier.padding(horizontal = 14.dp),
            color = Color.Gray.copy(alpha = 0.2f),
            thickness = 0.5.dp
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 48.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = label.annotatedBoldCompose(),
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.Medium,
            color = labelColor
        )
        value()
    }
}

@Composable
private fun RowScope.AppStatusTableValue(
    modifier: Modifier = Modifier,
    value: String,
    valueColor: Color = Color.Unspecified
) {
    Text(
        modifier = modifier.padding(start = 6.dp),
        text = value,
        style = MaterialTheme.typography.body1,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.End,
        color = valueColor
    )
}

@Composable
private fun AppStatusTableButton(
    text: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Text(
        modifier = Modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.accent.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        text = text,
        style = MaterialTheme.typography.body2,
        fontWeight = FontWeight.SemiBold,
        color = AppColors.accentDark
    )
}
