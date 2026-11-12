package app.paralino.core.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import app.paralino.core.presentation.settings.generic.GenericSettingListItem
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenStateContent
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenUiEvent
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.ConditionErrorItem
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.GenericSettingsOptions
import app.paralino.core.ui.components.LargeSeekbarSwitch
import app.paralino.core.ui.components.LargeSeekbarSwitchState
import app.paralino.core.ui.components.LargeSwitchSize
import app.paralino.core.ui.components.NoticeInfoChip
import app.paralino.core.ui.components.SettingsActionItem
import app.paralino.core.ui.components.SettingsSectionHeader
import app.paralino.core.ui.components.dialogs.LoadingDialog
import app.paralino.core.ui.components.rememberSeekbarState
import app.paralino.core.ui.groups.components.GroupMemberActionButton
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.annotatedBoldCompose

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GenericSettingsListScreen(
    coreUiState: CoreUiState,
    paddingValues: PaddingValues,
    state: GenericSettingsScreenStateContent.ListItems,
    mapContent: @Composable (GenericSettingListItem.Map) -> Unit,
    onEvent: (GenericSettingsScreenUiEvent) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = paddingValues,
        userScrollEnabled = state.isUserScrollEnabled
    ) {
        state.items.forEachIndexed { index, item ->
            when (item) {
                is GenericSettingListItem.GroupOptions -> {
                    this.item(key = item.keyId) {
                        GenericSettingsOptions(
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .fillMaxWidth()
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
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .fillMaxWidth()
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
                                .zIndex(item.zIndex)
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

                is GenericSettingListItem.DeletableItem -> {
                    this.item(key = item.keyId) {
                        GenericSettingDeletableItem(
                            modifier = Modifier
                                .zIndex(index.toFloat())
                                .animateItem()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            item = item,
                            onDelete = { onEvent(GenericSettingsScreenUiEvent.DeleteItem(item = item)) }
                        )
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

                is GenericSettingListItem.Map -> {
                    if (item.isVisible) {
                        this.item(key = item.keyId) {
                            GenericSettingMapItem(
                                state = item,
                                mapContent = mapContent
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
private fun GenericSettingDeletableItem(
    modifier: Modifier = Modifier,
    item: GenericSettingListItem.DeletableItem,
    onDelete: () -> Unit,
) {
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colors.surface,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    painter = item.icon.asPainterResource(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.subtitle2.copy(fontSize = 18.sp),
                )
            }

            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                item.tag?.let {
                    Text(
                        text = it.annotatedBoldCompose(),
                        style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                        color = if (item.isTagActive) {
                            MaterialTheme.colors.onPrimary
                        } else {
                            AppColors.darkBackground.copy(alpha = 0.8f)
                        },
                        modifier = Modifier
                            .background(
                                color = if (item.isTagActive) {
                                    AppColors.accent
                                } else {
                                    AppColors.highlightGold
                                },
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
                item.description?.let {
                    Text(
                        text = it.annotatedBoldCompose(),
                        style = MaterialTheme.typography.caption,
                    )
                }
            }
        }

        GroupMemberActionButton(
            startIcon = CoreAppIcons.Delete.asPainterResource(),
            endIcon = null,
            contentPadding = PaddingValues(6.dp),
            text = "",
            showText = false,
            backgroundColor = AppColors.errorBackground,
            contentColor = AppColors.redMain,
            onClick = onDelete
        )
    }
}

@Composable
fun GenericSettingMapItem(
    state: GenericSettingListItem.Map,
    mapContent: @Composable (GenericSettingListItem.Map) -> Unit
) {
    Box(
        modifier = Modifier
            .zIndex(10f)
            .fillMaxWidth()
            .height(state.height)
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp)
            .background(
                color = MaterialTheme.colors.background,
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                border = BorderStroke(6.dp, MaterialTheme.colors.surface),
                shape = RoundedCornerShape(20.dp)
            )
            .dropShadow(
                shape = RoundedCornerShape(20.dp),
                shadow = Shadow(
                    radius = 12.dp,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.2f),
                )
            )
            .clip(RoundedCornerShape(20.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = MaterialTheme.colors.background,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(4.dp)
                .clip(RoundedCornerShape(18.dp))
                .clipToBounds(),
        ) {
            mapContent(state)
        }
    }
}