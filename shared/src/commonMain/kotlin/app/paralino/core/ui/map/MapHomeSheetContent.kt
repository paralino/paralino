package app.paralino.core.ui.map

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SwipeableState
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.presentation.groups.GroupMemberItemState
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.places.list.PlaceListItemData
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.groups.components.GroupMember
import app.paralino.core.ui.places.PlaceListItem
import app.paralino.core.ui.theme.ButtonStyle
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun MapHomeSheetContent(
    appState: CoreUiState,
    pagerState: PagerState,
    swipeableState: SwipeableState<BottomSheetPosition>,
    readyState: MapHomeScreenState.Ready?,
    members: List<GroupMemberItemState>,
    places: List<PlaceListItemData>,
    onGroupClick: () -> Unit,
    onMemberClick: (GroupMemberItemState) -> Unit,
    onPlaceClick: (PlaceListItemData) -> Unit,
    onOpenPlaces: () -> Unit,
    onCreatePlace: () -> Unit,
    onWholeContentClick: () -> Unit,
    onExpandSheet: () -> Unit,
    onCollapseSheet: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onWholeContentClick()
            }
    ) {

        val contentAlpha by remember {
            derivedStateOf {
                if (swipeableState.progress.to == BottomSheetPosition.COLLAPSED) {
                    1f - swipeableState.progress.fraction
                } else if (swipeableState.progress.from == BottomSheetPosition.COLLAPSED) {
                    swipeableState.progress.fraction
                } else {
                    1f
                }
            }
        }

        Divider(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp, bottom = 12.dp)
                .clip(RoundedCornerShape(percent = 50))
                .width(30.dp),
            thickness = 6.dp
        )

        if (readyState != null) {
            MapGroupNameAndMemberInfo(
                modifier = Modifier.padding(horizontal = 20.dp),
                groupName = readyState.groupName,
                groupImage = readyState.groupImage,
                userCount = readyState.userCount,
                activeUserCount = readyState.activeUserCount,
                inactiveUserCount = readyState.inactiveUserCount,
                idleUserCount = readyState.inactiveUserCount,
                verified = readyState.allMembersVerified,
                interactionVisibilityFraction = contentAlpha,
                onGroupClick = {
                    if (swipeableState.isCollapsed()) {
                        appState.uiScope.launch {
                            swipeableState.animateTo(BottomSheetPosition.HALF_EXPANDED)
                        }
                    } else {
                        onGroupClick()
                    }
                }
            )
            Spacer(modifier = Modifier.size(12.dp))
        }

        TabRow(
            modifier = Modifier
                .height(40.dp)
                .fillMaxWidth(0.8f)
                .clip(RoundedCornerShape(50))
                .alpha(contentAlpha)
                .background(MaterialTheme.colors.surface)
                .align(Alignment.CenterHorizontally),
            backgroundColor = Color.Transparent,
            selectedTabIndex = pagerState.currentPage,
            indicator = { Box {} },
            divider = { Box {} }
        ) {
            MapBottomSheetTab(
                modifier = Modifier,
                selected = pagerState.currentPage == 0,
                text = MR.strings.group_details_section_members_title.localizedString(),
                icon = CoreAppIcons.Group.asPainterResource(),
                onClick = {
                    appState.uiScope.launch { onExpandSheet() }
                    appState.uiScope.launch { pagerState.animateScrollToPage(0) }
                }
            )
            MapBottomSheetTab(
                modifier = Modifier,
                selected = pagerState.currentPage == 1,
                text = MR.strings.places_list_screen_title.localizedString(),
                icon = CoreAppIcons.MapsHomeWork.asPainterResource(),
                onClick = {
                    appState.uiScope.launch { onExpandSheet() }
                    appState.uiScope.launch { pagerState.animateScrollToPage(1) }
                }
            )
        }

        Spacer(modifier = Modifier.size(12.dp))

        HorizontalPager(
            modifier = Modifier
                .weight(1f)
                .alpha(contentAlpha),
            state = pagerState,
            verticalAlignment = Alignment.Top
        ) {
            when (it) {
                0 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 80.dp)
                    ) {
                        this.itemsIndexed(
                            items = members,
                            key = { index, item -> item.keyId }
                        ) { index, itemState ->
                            if (index > 0)
                                Spacer(modifier = Modifier.size(8.dp))
                            GroupMember(
                                state = itemState,
                                backgroundColor = MaterialTheme.colors.surface,
                                collapseOnActionClick = true,
                                forceExpandAction = false,
                                onActionClick = null,
                                onClick = { onMemberClick(itemState) }
                            )
                        }
                    }
                }

                1 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        if (places.isEmpty()) {
                            this.item {
                                DefaultEmptyState(
                                    content = stringResource(MR.strings.places_list_screen_empty_info),
                                    icon = CoreAppIcons.MapsHomeWork.asPainterResource(),
                                    buttonText = stringResource(MR.strings.places_create_button_create),
                                    buttonIcon = CoreAppIcons.Add.asPainterResource()
                                ) {
                                    onCreatePlace()
                                }
                            }
                        } else {
                            this.itemsIndexed(
                                items = places,
                                key = { index, item -> item.id }
                            ) { index, itemState ->
                                if (index > 0)
                                    Spacer(modifier = Modifier.size(8.dp))
                                PlaceListItem(
                                    state = itemState,
                                    hasEndArrow = false,
                                    backgroundColor = MaterialTheme.colors.surface,
                                    onClick = { onPlaceClick(itemState) }
                                )
                            }
                            this.item(key = "places_list") {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    FilledButton(
                                        modifier = Modifier.padding(top = 20.dp),
                                        style = ButtonStyle.SMALL,
                                        icon = null,
                                        elevation = ButtonDefaults.elevation(
                                            0.dp,
                                            0.dp,
                                            0.dp,
                                            0.dp,
                                            0.dp
                                        ),
                                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.background),
                                        onClick = onOpenPlaces
                                    ) {
                                        Text(stringResource(MR.strings.button_showAll))
                                        Icon(
                                            modifier = Modifier
                                                .padding(start = 8.dp)
                                                .width(20.dp),
                                            painter = CoreAppIcons.ChevronRight.asPainterResource(),
                                            tint = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled),
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MapBottomSheetTab(
    modifier: Modifier = Modifier,
    selected: Boolean,
    text: String,
    icon: Painter,
    onClick: () -> Unit
) {
    Tab(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) MaterialTheme.colors.primary
                else MaterialTheme.colors.surface
            ),
        selected = selected,
        enabled = true,
        selectedContentColor = MaterialTheme.colors.onPrimary,
        unselectedContentColor = MaterialTheme.colors.onSurface,
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = icon,
                contentDescription = null
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = text, fontSize = 14.sp)
        }
    }
}