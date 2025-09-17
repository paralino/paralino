package app.paralino.android.ui.groups

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import app.paralino.core.MR
import app.paralino.core.presentation.groups.GroupItemState
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.components.BottomSheetPickerType
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.FullWidthActionButton
import app.paralino.core.ui.components.PlanLimitCount
import app.paralino.core.ui.components.SettingsSectionHeader
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.groups.GroupListItem
import app.paralino.core.ui.theme.AppColors
import dev.icerock.moko.resources.compose.stringResource


@Composable
fun GroupBottomSheetPicker(
    modifier: Modifier = Modifier,
    state: GroupBottomSheetPickerState,
    onClick: (GroupPickerAction) -> Unit
) {
    ModalBottomSheetLayout(
        modifier = modifier.fillMaxSize(),
        sheetBackgroundColor = MaterialTheme.colors.background,
        sheetShape = RoundedCornerShape(topStartPercent = 5, topEndPercent = 5),
        sheetState = state.bottomSheetState,
        scrimColor = MaterialTheme.colors.surface.copy(alpha = 0.6f),
        sheetContent = {
            Spacer(modifier = Modifier.height(1.dp))
            GroupPickerScreen(
                state = state.state,
                onClick = onClick
            )
        },
        content = {}
    )
}

class GroupBottomSheetPickerState(
    val bottomSheetState: ModalBottomSheetState
) {
    var state by mutableStateOf<BottomSheetPickerType.GroupPickerBottomSheet?>(null)

    val isVisible: Boolean get() = bottomSheetState.isVisible

    suspend fun show(state: BottomSheetPickerType.GroupPickerBottomSheet) {
        this.state = state
        bottomSheetState.show()
    }

    suspend fun hide() {
        bottomSheetState.hide()
    }
}

@Composable
fun rememberGroupBottomSheetPickerState(
    initialValue: ModalBottomSheetValue = ModalBottomSheetValue.Hidden
): GroupBottomSheetPickerState {
    val bottomSheetState = rememberModalBottomSheetState(
        initialValue = initialValue,
        skipHalfExpanded = true
    )
    return remember(Unit) {
        GroupBottomSheetPickerState(bottomSheetState)
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupPickerScreen(
    state: BottomSheetPickerType.GroupPickerBottomSheet?,
    onClick: (GroupPickerAction) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 30.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        stickyHeader(key = "title") {
            DefaultToolbar(
                modifier = Modifier
                    .padding(bottom = 0.dp, top = 10.dp)
                    .padding(horizontal = 10.dp),
                elevation = 0.dp,
                titleComposable = if (state?.personalSpace == null || !state.showPersonalSpace) {
                    {
                        GroupTitle(state = state)
                    }
                } else {
                    null
                },
                navigationIcon = { ToolbarButtons.Close(onClick = { onClick(GroupPickerAction.Close) }) },
                actions = {}
            )
        }

        if (state != null) {

            if (state.personalSpace != null && state.showPersonalSpace) {
                item(key = "personal_space") {
                    GroupListItem(
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .clickable { onClick(GroupPickerAction.GroupPicked(state.personalSpace!!)) },
                        data = state.personalSpace!!,
                        onEdit = { onClick(GroupPickerAction.GroupEditPicked(state.personalSpace!!)) }
                    )
                }

                stickyHeader(key = "groups_title") {
                    CompositionLocalProvider(
                        LocalContentAlpha provides ContentAlpha.high,
                        content = {
                            ProvideTextStyle(
                                value = MaterialTheme.typography.h6.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 4.dp)
                                        .padding(top = 20.dp, bottom = 8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    GroupTitle(state = state)
                                }
                            }
                        }
                    )
                }
            }

            groupSection(
                id = "created_groups_section",
                title = MR.strings.dashboard_groups_title_created.localizedString(),
                groupsLimit = state.groupsLimit,
                onlyShowList = !state.onlyShowGroups,
                groups = state.createdGroups,
                onClick = onClick
            )

            if (state.createdGroups.isNotEmpty() && state.onlyShowGroups) {
                item {
                    Spacer(Modifier.size(20.dp))
                }
            }

            groupSection(
                id = "joined_groups_section",
                title = MR.strings.dashboard_groups_title_joined.localizedString(),
                groupsLimit = null,
                onlyShowList = !state.onlyShowGroups,
                groups = state.joinedGroups,
                onClick = onClick
            )

            if (!state.onlyShowGroups) {
                groupCreateSection(
                    id = "group_create",
                    canCreateGroup = state.canCreateGroup,
                    groupsCount = state.createdGroups.size,
                    groupsLimit = state.groupsLimit,
                    onClick = onClick
                )
            }
        }
    }
}

@Composable
private fun GroupTitle(
    modifier: Modifier = Modifier,
    state: BottomSheetPickerType.GroupPickerBottomSheet?,
) {
    Row(
        modifier = Modifier.padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(MR.strings.title_dashboard),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.size(8.dp))
        if (state != null) {
            PlanLimitCount(
                count = state.createdGroups.size + state.joinedGroups.size,
                limit = null
            )
        }
    }
}

private fun LazyListScope.groupCreateSection(
    id: String,
    canCreateGroup: Boolean,
    groupsCount: Int,
    groupsLimit: Int?,
    onClick: (GroupPickerAction) -> Unit,
) {
    item(id) {
        Column(
            modifier = Modifier.padding(top = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .zIndex(1f)
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 20.dp)
                    .clickable { onClick(GroupPickerAction.NewGroup) }
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colors.background)
                    .background(
                        if (canCreateGroup) {
                            AppColors.accentLight.copy(alpha = 0.2f)
                        } else {
                            AppColors.errorBackground//.copy(alpha = 0.1f)
                        }
                    )
                    .padding(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = CoreAppIcons.Add.asPainterResource(),
                        modifier = Modifier.size(20.dp),
                        tint = if (isSystemInDarkTheme()) AppColors.accent else AppColors.accentDark,
                        contentDescription = null
                    )
                    Text(
                        text = stringResource(MR.strings.dashboard_groups_add_button),
                        color = if (isSystemInDarkTheme()) AppColors.accent else AppColors.accentDark,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .weight(1f)
                    )
                    PlanLimitCount(
                        count = groupsCount,
                        limit = groupsLimit,
                        backgroundColor = Color.Transparent,
                        contentColor = if (isSystemInDarkTheme()) AppColors.accent else AppColors.accentDark
                    )
                }
                if (!canCreateGroup) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onClick(GroupPickerAction.OpenSubscriptions) }
                            .clip(RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(MR.strings.dashboard_groups_create_plan_limit),
                            color = AppColors.redMain,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    color = AppColors.lightBackground,
                                    shape = CircleShape
                                )
                                .padding(2.dp),
                            painter = CoreAppIcons.ChevronRight.asPainterResource(),
                            contentDescription = null,
                            tint = MaterialTheme.colors.primary,
                        )
                    }
                }
            }
            FullWidthActionButton(
                modifier = Modifier
                    .zIndex(0f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp),
                startIcon = CoreAppIcons.AddLink.asPainterResource(),
                endIcon = CoreAppIcons.ChevronRight.asPainterResource(),
                text = stringResource(MR.strings.dashboard_groups_join_button),
                onClick = {
                    onClick(GroupPickerAction.JoinGroup)
                }
            )
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.groupSection(
    id: String,
    title: String,
    groupsLimit: Int?,
    onlyShowList: Boolean,
    groups: List<GroupItemState>,
    onClick: (GroupPickerAction) -> Unit
) {
    if (groups.isNotEmpty() && !onlyShowList) {
        stickyHeader(key = id) {
            SettingsSectionHeader(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
                name = title,
                count = groups.size,
                limit = groupsLimit
            )
        }
    }

    items(groups, key = { it.id.id }) { group ->
        GroupListItem(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .clickable { onClick(GroupPickerAction.GroupPicked(group)) },
            data = group,
            onEdit = { onClick(GroupPickerAction.GroupEditPicked(group)) }
        )
    }
}

sealed interface GroupPickerAction {
    object NewGroup : GroupPickerAction
    object JoinGroup : GroupPickerAction
    object Close : GroupPickerAction
    object OpenSubscriptions : GroupPickerAction
    data class GroupPicked(val group: GroupItemState) : GroupPickerAction
    data class GroupEditPicked(val group: GroupItemState) : GroupPickerAction
}