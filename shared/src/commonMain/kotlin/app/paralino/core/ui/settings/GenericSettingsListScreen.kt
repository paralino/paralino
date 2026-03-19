package app.paralino.core.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.CircularProgressIndicator
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
import androidx.compose.ui.zIndex
import app.paralino.core.models.base.DeviceType
import app.paralino.core.presentation.settings.generic.GenericSettingListItem
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenStateContent
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenUiEvent
import app.paralino.core.presentation.settings.generic.GroupOptionItem
import app.paralino.core.provider.current
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.CheckButtonIcon
import app.paralino.core.ui.components.ConditionErrorItem
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.LargeSeekbarSwitch
import app.paralino.core.ui.components.LargeSeekbarSwitchState
import app.paralino.core.ui.components.LargeSwitchSize
import app.paralino.core.ui.components.NoticeInfoChip
import app.paralino.core.ui.components.SettingsSectionHeader
import app.paralino.core.ui.components.SmallActionButton
import app.paralino.core.ui.components.dialogs.LoadingDialog
import app.paralino.core.ui.components.rememberSeekbarState
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.annotatedBoldCompose

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GenericSettingsListScreen(
    coreUiState: CoreUiState,
    paddingValues: PaddingValues,
    state: GenericSettingsScreenStateContent.ListItems,
    onEvent: (GenericSettingsScreenUiEvent) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = paddingValues
    ) {
        state.items.forEachIndexed { index, item ->
            when (item) {
                is GenericSettingListItem.GroupOptions -> {
                    this.item(key = item.keyId) {
                        GenericSettingsOptions(
                            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()
                                .animateItem(),
                            items = item.visibleItems,
                            onOptionSelected = { option ->
                                onEvent(
                                    GenericSettingsScreenUiEvent.SelectOptionItem(
                                        groupOptions = item,
                                        option = option
                                    )
                                )
                            }
                        )
                    }
                }

                is GenericSettingListItem.Notice -> {
                    this.item(key = item.keyId) {
                        NoticeInfoChip(
                            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()
                                .animateItem(),
                            icon = item.icon.asPainterResource(),
                            text = item.content
                        )
                    }
                }

                is GenericSettingListItem.Spacer -> {
                    this.item(key = item.keyId) {
                        Spacer(modifier = Modifier.size(item.spacing).animateItem())
                    }
                }

                is GenericSettingListItem.ConditionError -> {
                    this.item(key = item.keyId) {
                        ConditionErrorItem(
                            Modifier
                                .zIndex(0f)
                                .padding(horizontal = 20.dp)
                                .animateItem(),
                            data = item.type,
                            onClick = {
                                onEvent(GenericSettingsScreenUiEvent.ConditionErrorItemClick(item = item))
                            }
                        )
                    }
                }

                is GenericSettingListItem.StickyHeader -> {
                    this.stickyHeader(key = item.keyId) {
                        SettingsSectionHeader(
                            modifier = Modifier
                                .zIndex(0f)
                                .padding(horizontal = 20.dp)
                                .fillMaxWidth()
                                .animateItem(),
                            name = item.content
                        )
                    }
                }

                is GenericSettingListItem.Subtitle -> {
                    this.item(key = item.keyId) {
                        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                            Text(
                                modifier = Modifier
                                    .padding(bottom = item.bottomPadding)
                                    .padding(horizontal = 20.dp)
                                    .fillMaxWidth()
                                    .animateItem(),
                                text = item.content.annotatedBoldCompose(),
                                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }

                is GenericSettingListItem.Switch<*> -> {
                    if (item.isVisible) {
                        this.item(key = item.keyId) {
                            LargeSeekbarSwitch(
                                modifier = Modifier
                                    .zIndex(index.toFloat())
                                    .fillMaxWidth()
                                    .animateItem()
                                    .padding(bottom = 20.dp)
                                    .padding(horizontal = 20.dp),
                                icon = item.icon?.asPainterResource(),
                                title = item.title,
                                description = item.description,
                                enabled = true,
                                size = LargeSwitchSize(
                                    minHeight = 60.dp
                                ),
                                state = rememberSeekbarState(
                                    key = item,
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

                is GenericSettingListItem.SettingsActionItem -> {
                    if (item.isVisible) {
                        this.item(key = item.keyId) {
                            SettingsActionItem(
                                modifier = Modifier
                                    .zIndex(index.toFloat())
                                    .fillMaxWidth()
                                    .animateItem()
                                    .padding(horizontal = 8.dp),
                                name = item.text,
                                icon = item.icon.asPainterResource(),
                                description = item.description,
                                tag = item.tag,
                                type = item.type,
                                onClick = {
                                    onEvent(GenericSettingsScreenUiEvent.SelectActionItem(item = item))
                                }
                            )
                        }
                    }
                }

                is GenericSettingListItem.EmptyState -> {
                    if (item.isVisible) {
                        this.item(key = item.keyId) {
                            DefaultEmptyState(
                                modifier = Modifier,
                                icon = item.icon.asPainterResource(),
                                content = item.content,
                                buttonText = item.buttonText,
                                buttonState = AppButtonState.loadingStateWithCheck(item.buttonLoading),
                                onClick = { onEvent(GenericSettingsScreenUiEvent.GlobalEmptyStateActionClick) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.isFullScreenLoading) {
        LoadingDialog(text = state.loadingText)
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
                batteryImpact = state.tag,
                highBatteryImpact = state.isTagWarning,
                isSelected = state.isSelected,
                isLoading = state.isLoading,
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
    isLoading: Boolean,
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
        Box(
            modifier = Modifier
                .size(24.dp)
        ) {
            androidx.compose.animation.AnimatedVisibility(
                modifier = Modifier.fillMaxSize(),
                visible = isLoading,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                CircularProgressIndicator(
                    color = contentColor,
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                )
            }
            androidx.compose.animation.AnimatedVisibility(
                modifier = Modifier.fillMaxSize(),
                visible = !isLoading,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                CheckButtonIcon(
                    modifier = Modifier.fillMaxSize(),
                    isChecked = isSelected,
                    backgroundCheckedColor = MaterialTheme.colors.onPrimary,
                    contentCheckedColor = MaterialTheme.colors.primary,
                    backgroundUncheckedColor = backgroundColor,
                    contentUncheckedColor = contentColor.copy(alpha = ContentAlpha.disabled)
                )
            }
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