package app.paralino.core.ui.personal_space

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.models.location.MemberDeviceOnlineStatusType
import app.paralino.core.models.location.MemberDeviceStatusType
import app.paralino.core.presentation.personal_space.UserDeviceItemEvent
import app.paralino.core.presentation.personal_space.UserDeviceOrSessionItemState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.groups.GroupStatusTagContent
import app.paralino.core.ui.groups.components.GroupMemberActionButton
import app.paralino.core.ui.groups.components.NetworkTypeIcon
import app.paralino.core.ui.groups.toStatusTagType
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.annotatedBoldCompose
import app.paralino.core.utils.extensions.conditional
import app.paralino.core.utils.extensions.pulsate
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun UserDeviceItem(
    modifier: Modifier = Modifier,
    elevation: Dp = 0.dp,
    state: UserDeviceOrSessionItemState,
    onEvent: (UserDeviceItemEvent) -> Unit
) {
    var expanded by rememberSaveable(state.id) {
        mutableStateOf(false)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
//                onEvent(UserDeviceItemEvent.onItemClick(state))
                expanded = !expanded
            }
            .conditional(elevation.value > 0) {
                shadow(elevation = elevation, shape = RoundedCornerShape(12.dp))
            }
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.surface)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
    ) {
        Column(modifier = Modifier) {
            Row(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Device icon with status indicator
                Box(modifier = Modifier.width(IntrinsicSize.Min)) {
                    Column(
                        modifier = Modifier.width(50.dp)
                    ) {
                        if (state.device != null) {
                            Icon(
                                modifier = Modifier
                                    .size(50.dp)
                                    .border(
                                        width = if (state.device?.isCurrentDevice == true) 2.dp else 0.5.dp,
                                        color = if (state.device?.isCurrentDevice == true) {
                                            MaterialTheme.colors.primary
                                        } else {
                                            MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled)
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp),
                                painter = CoreAppIcons.PhoneAndroid.asPainterResource(),
                                contentDescription = null
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .border(
                                        width = if (state.device?.isCurrentDevice == true) 2.dp else 0.5.dp,
                                        color = if (state.device?.isCurrentDevice == true) {
                                            MaterialTheme.colors.primary
                                        } else {
                                            MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled)
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "?",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.h5,
                                    color = MaterialTheme.colors.onSurface
                                )
                            }
                        }

                        // Battery level
                        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                            if (state.device?.visibleBatteryLevel != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(width = 16.dp, height = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            modifier = Modifier
                                                .requiredSize(16.dp)
                                                .rotate(90f),
                                            painter = if (state.device?.location?.isBatteryCharging == true) {
                                                CoreAppIcons.BatteryChargingFull.asPainterResource()
                                            } else {
                                                CoreAppIcons.BatteryFull.asPainterResource()
                                            },
                                            tint = when {
                                                state.device?.visibleBatteryLevel!! > 85 -> AppColors.accent
                                                state.device?.visibleBatteryLevel!! < 15 -> AppColors.redMain
                                                state.device?.visibleBatteryLevel!! < 30 -> AppColors.highlightGold
                                                else -> MaterialTheme.colors.onSurface
                                            },
                                            contentDescription = null
                                        )
                                    }
                                    Spacer(modifier = Modifier.size(2.dp))
                                    Text(
                                        text = "${state.device?.visibleBatteryLevel}%",
                                        style = MaterialTheme.typography.overline.copy(fontWeight = FontWeight.Medium),
                                        color = when {
                                            state.device?.visibleBatteryLevel!! < 15 -> AppColors.redMain
                                            state.device?.visibleBatteryLevel!! < 30 -> AppColors.highlightGold
                                            else -> MaterialTheme.colors.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Online status indicator
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .conditional(
                                value = state.device?.location?.onlineStatus == MemberDeviceOnlineStatusType.ONLINE
                            ) {
                                pulsate()
                            }
                            .align(Alignment.TopStart)
                            .offset(x = (-4).dp, y = (-4).dp)
                            .clip(CircleShape)
                            .border(
                                border = BorderStroke(
                                    width = 2.dp,
                                    color = when (state.device?.location?.onlineStatus) {
                                        MemberDeviceOnlineStatusType.UNKNOWN,
                                        null -> Color.Transparent

                                        else -> MaterialTheme.colors.surface
                                    },
                                ),
                                shape = CircleShape
                            )
                            .padding(1.dp)
                            .background(
                                color = when (state.device?.location?.onlineStatus) {
                                    MemberDeviceOnlineStatusType.ONLINE -> AppColors.accent
                                    MemberDeviceOnlineStatusType.BUSY -> AppColors.highlightGold
                                    MemberDeviceOnlineStatusType.OFFLINE -> Color.Gray
                                    MemberDeviceOnlineStatusType.UNKNOWN,
                                    null -> Color.Transparent
                                },
                                shape = CircleShape
                            )
                    )
                }

                // Device info
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 8.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = when {
                            state.device?.isCurrentDevice == true -> {
                                if (!state.device?.name.isNullOrBlank()) {
                                    stringResource(MR.strings.personalSpace_current_device_withName, state.device?.name ?: "")
                                } else {
                                    stringResource(MR.strings.personalSpace_current_device)
                                }
                            }
                            !state.device?.name.isNullOrBlank() -> state.device?.name ?: ""
                            state.session != null -> state.session?.session?.deviceOrClientName ?: "-"
                            else -> stringResource(MR.strings.group_members_device_name_default)
                        }.annotatedBoldCompose(
                            textColor = MaterialTheme.colors.onSurface
                        ),
                        style = MaterialTheme.typography.subtitle1
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
                        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
                    ) {
                        if (state.device != null) {
                            GroupStatusTagContent((state.device?.location?.deviceStatus ?: MemberDeviceStatusType.INACTIVE).toStatusTagType())

                            if (state.device?.device?.deviceType != null) {
                                Text(
                                    text = state.device?.device?.deviceType?.toString() ?: "",
                                    style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colors.onSecondary,
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colors.secondary,
                                            shape = MaterialTheme.shapes.medium
                                        )
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                        .align(Alignment.CenterVertically)
                                )
                            }
                        }

                        if (state.session == null) {
                            Text(
                                text = stringResource(MR.strings.personalSpace_device_tag_loggedOut),
                                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                                color = AppColors.darkBackground.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .background(
                                        AppColors.highlightGold,
                                        MaterialTheme.shapes.medium
                                    )
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                                    .align(Alignment.CenterVertically)
                            )
                        }

                        if (state.device == null) {
                            Text(
                                text = stringResource(MR.strings.personalSpace_device_tag_unknownSession),
                                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                                color = AppColors.darkBackground.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .background(
                                        AppColors.highlightGold,
                                        MaterialTheme.shapes.medium
                                    )
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                                    .align(Alignment.CenterVertically)
                            )
                        }

                        if (state.session?.isSessionExpired == true) {
                            Text(
                                text = stringResource(MR.strings.personalSpace_device_tag_expiredSession),
                                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                                color = AppColors.darkBackground.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .background(
                                        AppColors.highlightGold,
                                        MaterialTheme.shapes.medium
                                    )
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                                    .align(Alignment.CenterVertically)
                            )
                        }
                    }

                    // Last location ping
                    if (state.device?.location?.lastLocationPing != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = CoreAppIcons.AccessTime.asPainterResource(),
                                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = 2.dp)
                                    .size(16.dp)
                            )
                            Text(
                                text = state.device?.location?.lastLocationPing?.annotatedBoldCompose() ?: "".annotatedBoldCompose(),
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                            )
                        }
                    }

                    // Speed, elevation, network info
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.device?.location?.networkType != null) {
                            NetworkTypeIcon(
                                networkType = state.device?.location?.networkType!!,
                                signalStrength = state.device?.location?.signalStrength
                            )
                            Text(
                                text = state.device?.location?.networkType?.networkName!!,
                                style = MaterialTheme.typography.overline.copy(letterSpacing = 0.1.sp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                    }
                }
            }

            // Actions section
            FlowRow(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .padding(bottom = 10.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.device?.deleteAction != null) {
                    GroupMemberActionButton(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        startIcon = CoreAppIcons.Delete.asPainterResource(),
                        endIcon = null,
                        isLoading = false,
                        text = stringResource(MR.strings.button_delete),
                        backgroundColor = AppColors.errorBackground,
                        contentColor = AppColors.redMain,
                        onClick = {
                            onEvent(UserDeviceItemEvent.onDeleteDeviceClick(state.device!!))
                        }
                    )
                }

                if (state.device?.deactivateAction != null) {
                    GroupMemberActionButton(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        startIcon = CoreAppIcons.LocationOff.asPainterResource(),
                        endIcon = null,
                        isLoading = false,
                        text = stringResource(MR.strings.personalSpace_device_deactivate_action),
                        backgroundColor = AppColors.errorBackground,
                        contentColor = AppColors.redMain,
                        onClick = {
                            onEvent(UserDeviceItemEvent.onDeactivateDeviceClick(state.device!!))
                        }
                    )
                }

                if (state.session != null) {
                    GroupMemberActionButton(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        startIcon = CoreAppIcons.PrivacyTip.asPainterResource(),
                        endIcon = CoreAppIcons.ChevronRight.asPainterResource(),
                        text = stringResource(MR.strings.personalSpace_session_title),
                        backgroundColor = if (isSystemInDarkTheme()) {
                            AppColors.accentLight.copy(alpha = 0.2f)
                        } else {
                            AppColors.accent.copy(alpha = 0.2f)
                        },
                        onClick = {
                            onEvent(UserDeviceItemEvent.onOpenSessionClick(state.session!!.session))
                        }
                    )
                }
            }
        }
    }
} 