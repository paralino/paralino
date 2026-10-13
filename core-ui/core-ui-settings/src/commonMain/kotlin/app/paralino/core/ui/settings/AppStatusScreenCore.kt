package app.paralino.core.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
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
import app.paralino.core.MR
import app.paralino.core.models.location.LocationProviderType
import app.paralino.core.presentation.common.endActionTextRes
import app.paralino.core.presentation.common.nameRes
import app.paralino.core.presentation.common.resolvedNameRes
import app.paralino.core.presentation.settings.status.AppStatusAllowanceState
import app.paralino.core.presentation.settings.status.AppStatusConnectionState
import app.paralino.core.presentation.settings.status.AppStatusCurrentLocationState
import app.paralino.core.presentation.settings.status.AppStatusLocationModeState
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
            AppStatusConnectionTable(connection = connection)
        }

        uiState.locationMode?.let { locationMode ->
            SettingsSectionHeader(name = stringResource(MR.strings.setting_status_section_locationMode))
            AppStatusLocationMode(locationMode = locationMode)
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

        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
                modifier = Modifier.padding(top = 16.dp, start = 4.dp, end = 4.dp),
                text = stringResource(MR.strings.setting_status_refreshInfo),
                style = MaterialTheme.typography.caption
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
private fun AppStatusConnectionTable(connection: AppStatusConnectionState) {
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
    }
}

@Composable
private fun AppStatusLocationMode(locationMode: AppStatusLocationModeState) {
    Text(
        text = locationMode.current,
        style = MaterialTheme.typography.h5,
        fontWeight = FontWeight.Bold
    )
    locationMode.requested?.let { requested ->
        CompositionLocalProvider(LocalContentAlpha provides if (locationMode.isThrottled) 1f else ContentAlpha.medium) {
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = stringResource(MR.strings.setting_status_locationMode_requested, requested),
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Medium,
                color = if (locationMode.isThrottled) AppColors.redMain else Color.Unspecified
            )
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
        if (allowance.remainingFraction != null) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                progress = allowance.remainingFraction ?: 0f,
                color = color,
                backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
            )
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
            .heightIn(min = 48.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = label,
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.Medium
        )
        leadingValue?.invoke()
        Text(
            modifier = Modifier.padding(start = 6.dp),
            text = value,
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            color = valueColor
        )
    }
}
