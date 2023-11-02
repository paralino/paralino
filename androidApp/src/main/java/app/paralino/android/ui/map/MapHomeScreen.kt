package app.paralino.android.ui.map

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.groups.GroupInitScreen
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.ui.places.list.PlaceListItem
import app.paralino.android.ui.theme.AppIcons
import app.paralino.core.MR
import app.paralino.core.models.Resource
import app.paralino.core.presentation.groups.GroupMemberItemState
import app.paralino.core.presentation.map.MapHomeScreenFocusType
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapHomeViewModel
import app.paralino.core.presentation.map.asFocusedMarker
import app.paralino.core.presentation.places.list.PlaceListItemData
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.groups.components.GroupMember
import app.paralino.core.ui.theme.ButtonStyle
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel

private val sheetPeekHeight = 84.dp

@Composable
fun MapHomeScreen(
    appState: MainAppState,
    groupId: String?,
    vm: MapHomeViewModel = getViewModel(),
    onClose: () -> Unit,
) {
    MessageDispatcherEffect(vm, appState)

    val groupState by vm.allGroups.collectAsState()
    when (val state = groupState) {
        Resource.NotInitialized -> {}
        is Resource.Ready -> {
            if (state.data.isNotEmpty()) {
                MapHomeContent(
                    appState = appState,
                    startGroupId = groupId,
                    vm = vm
                )
            } else {
                GroupInitScreen(
                    appState = appState,
                    onClose = onClose
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun MapHomeContent(
    appState: MainAppState,
    startGroupId: String?,
    vm: MapHomeViewModel,
) {

    val uiState by vm.uiState.collectAsState(MapHomeScreenState.Empty())
    val readyState by rememberUpdatedState(newValue = uiState as? MapHomeScreenState.Ready)
    val scaffoldState = rememberBottomSheetScaffoldState()
    val pagerState = rememberPagerState { 2 }

    rememberSaveable(startGroupId) {
        vm.switchGroup(groupId = startGroupId ?: "")
        ""
    }

    BackHandler(
        enabled = readyState?.mapHomeScreenFocusType?.isMemberOrPlaceFocused == true || scaffoldState.bottomSheetState.isExpanded
    ) {
        appState.uiScope.launch {
            scaffoldState.bottomSheetState.collapse()
        }
        vm.hideFocusedItem()
    }

    LaunchedEffect(readyState?.mapHomeScreenFocusType) {
        snapshotFlow { readyState?.mapHomeScreenFocusType }
            .distinctUntilChanged()
            .onEach {
                when (it) {
                    MapHomeScreenFocusType.ALL_MEMBERS -> {
                        pagerState.scrollToPage(0)
                    }

                    MapHomeScreenFocusType.ALL_PLACES -> {
                        pagerState.scrollToPage(1)
                    }

                    else -> {}
                }
            }
            .filter { it?.isMemberOrPlaceFocused == true }
            .distinctUntilChanged()
            .onEach { scaffoldState.bottomSheetState.collapse() }
            .collect()
    }

    val sheetHeight by rememberUpdatedState(newValue = if (readyState?.mapHomeScreenFocusType?.isMemberOrPlaceFocused == true) 0.dp else sheetPeekHeight)

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetElevation = 8.dp,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetPeekHeight = sheetHeight,
        sheetGesturesEnabled = true,
        sheetBackgroundColor = MaterialTheme.colors.background,
        sheetContent = {
            MapHomeSheetContent(
                appState = appState,
                bottomSheetState = scaffoldState.bottomSheetState,
                pagerState = pagerState,
                members = readyState?.members ?: emptyList(),
                places = readyState?.places ?: emptyList(),
                onMemberClick = {
                    vm.trackMarker(it.asFocusedMarker())
                },
                onPlaceClick = {
                    vm.trackMarker(it.asFocusedMarker())
                },
                onOpenPlaces = {
                    appState.navController.navigate(
                        AppScreen.MainHome.PlacesList.route(
                            groupId = readyState?.groupId ?: ""
                        )
                    )
                },
                onCreatePlace = {
                    appState.navController.navigate(
                        AppScreen.MainHome.PlaceCreate.route(
                            groupId = readyState?.groupId ?: "",
                            placeId = null
                        )
                    )
                }
            )
        },
        content = {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val bottomOffset = this.maxHeight - LocalDensity.current.run {
                    scaffoldState.bottomSheetState.requireOffset().toDp()
                }
                MainHomeMapComposable(
                    appState = appState,
                    uiState = uiState,
                    vm = vm,
                    mapContentPadding = PaddingValues(
                        top = 68.dp,
                        bottom = if (readyState?.mapHomeScreenFocusType?.isMemberOrPlaceFocused == true) {
                            0.dp
                        } else {
                            bottomOffset
                        }
                    )
                )
                MapHomeScreenElements(
                    appState = appState,
                    uiState = uiState,
                    sheetPeekHeight = bottomOffset,
                    vm = vm
                )
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun MapHomeSheetContent(
    appState: MainAppState,
    bottomSheetState: androidx.compose.material.BottomSheetState,
    pagerState: PagerState,
    members: List<GroupMemberItemState>,
    places: List<PlaceListItemData>,
    onMemberClick: (GroupMemberItemState) -> Unit,
    onPlaceClick: (PlaceListItemData) -> Unit,
    onOpenPlaces: () -> Unit,
    onCreatePlace: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxHeight(0.5f)
    ) {
        Divider(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp, bottom = 12.dp)
                .clip(RoundedCornerShape(percent = 50))
                .width(30.dp)
                .clickable {
                    appState.uiScope.launch {
                        bottomSheetState.expand()
                    }
                },
            thickness = 6.dp
        )
        TabRow(
            modifier = Modifier
                .height(40.dp)
                .fillMaxWidth(0.8f)
                .clip(RoundedCornerShape(50))
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
                    appState.uiScope.launch { bottomSheetState.expand() }
                    appState.uiScope.launch { pagerState.animateScrollToPage(0) }
                }
            )
            MapBottomSheetTab(
                modifier = Modifier,
                selected = pagerState.currentPage == 1,
                text = MR.strings.places_list_screen_title.localizedString(),
                icon = CoreAppIcons.MapsHomeWork.asPainterResource(),
                onClick = {
                    appState.uiScope.launch { bottomSheetState.expand() }
                    appState.uiScope.launch { pagerState.animateScrollToPage(1) }
                }
            )
        }

        Spacer(modifier = Modifier.size(12.dp))

        HorizontalPager(
            modifier = Modifier.weight(1f),
            state = pagerState,
            verticalAlignment = Alignment.Top
        ) {
            when (it) {
                0 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
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
                                    content = stringResource(id = MR.strings.places_list_screen_empty_info.resourceId),
                                    icon = AppIcons.MapsHomeWork,
                                    buttonText = stringResource(id = MR.strings.places_create_button_create.resourceId),
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
                                        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.background),
                                        onClick = onOpenPlaces
                                    ) {
                                        Text(stringResource(MR.strings.button_showAll.resourceId))
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
