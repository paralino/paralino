package app.paralino.core.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import app.paralino.core.models.base.DeviceType
import app.paralino.core.presentation.settings.generic.GenericSettingListItem
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenState
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenUiEvent
import app.paralino.core.presentation.settings.generic.GroupOptionItem
import app.paralino.core.provider.current
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.LargeSeekbarSwitch
import app.paralino.core.ui.components.LargeSeekbarSwitchState
import app.paralino.core.ui.components.NoticeInfoChip
import app.paralino.core.ui.components.SettingsSectionHeader
import app.paralino.core.ui.components.SmallActionButton
import app.paralino.core.ui.components.rememberSeekbarState
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.annotatedBoldCompose

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GenericSettingsListScreen(
    coreUiState: CoreUiState,
    paddingValues: PaddingValues,
    state: GenericSettingsScreenState.ListItems,
    onEvent: (GenericSettingsScreenUiEvent) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = paddingValues
    ) {
        state.items.forEach { item ->
            when (item) {
                is GenericSettingListItem.GroupOptions -> {
                    this.item(key = item.keyId) {
                        GenericSettingsOptions(
                            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                            items = item.items,
                            onOptionSelected = {
                                onEvent(
                                    GenericSettingsScreenUiEvent.SelectOptionItem(it)
                                )
                            }
                        )
                    }
                }
                is GenericSettingListItem.Notice -> {
                    this.item(key = item.keyId) {
                        NoticeInfoChip(
                            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                            icon = item.icon.asPainterResource(),
                            text = item.content
                        )
                    }
                }
                is GenericSettingListItem.Spacer -> {
                    this.item(key = item.keyId) {
                        Spacer(modifier = Modifier.size(item.spacing))
                    }
                }
                is GenericSettingListItem.StickyHeader -> {
                    this.stickyHeader(key = item.keyId) {
                        SettingsSectionHeader(
                            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                            name = item.content
                        )
                    }
                }
                is GenericSettingListItem.Subtitle -> {
                    this.item(key = item.keyId) {
                        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                            Text(
                                modifier = Modifier
                                    .padding(bottom = 20.dp)
                                    .padding(horizontal = 20.dp)
                                    .fillMaxWidth(),
                                text = item.content.annotatedBoldCompose(),
                                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
                is GenericSettingListItem.Switch<*> -> {
                    this.item(key = item.keyId) {
                        LargeSeekbarSwitch(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp),
                            icon = item.icon.asPainterResource(),
                            title = item.title,
                            description = item.description,
                            state = rememberSeekbarState(
                                key = state,
                                state = LargeSeekbarSwitchState.SeekbarSwitchState(
                                    seekbarState = null,
                                    switchState = LargeSeekbarSwitchState.Switch(
                                        checkState = item.isChecked,
                                        loadingState = item.isLoading
                                    )
                                )
                            ).value,
                            onStateChange = {
                                onEvent(GenericSettingsScreenUiEvent.ToggleSwitch(switch = item))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GenericSettingsOptions(
    modifier: Modifier = Modifier,
    items: List<GroupOptionItem>,
    onOptionSelected: (GroupOptionItem) -> Unit
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

            GenericSettingsOptionItem(
                title = state.title,
                description = state.description,
                batteryImpact = state.batteryImpact,
                highBatteryImpact = state.highBatteryImpact,
                isSelected = state.isSelected,
                isPremiumFeatureEnabled = state.isPremiumFeatureEnabled,
                onClick = { onOptionSelected(state) }
            )
        }
    }
}

@Composable
fun GenericSettingsOptionItem(
    modifier: Modifier = Modifier,
    title: String,
    description: String?,
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
                    .align(Alignment.CenterVertically)
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
                    .align(Alignment.CenterVertically)
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
                .align(Alignment.CenterVertically)
                .alpha(if (isPremiumFeatureEnabled) 1f else 0.5f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1.copy(
                    color = contentColor,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = if (DeviceType.current == DeviceType.iOS) 18.sp else 24.sp,
                )
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

            if (description != null) {
                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                    Text(
                        modifier = Modifier.padding(top = 2.dp),
                        text = description.annotatedBoldCompose(),
                        style = MaterialTheme.typography.caption,
                        textAlign = TextAlign.Start,
                        color = contentColor
                    )
                }
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