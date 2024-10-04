package app.paralino.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberSwipeableState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import app.paralino.core.presentation.groups.CreateGroupViewModel
import app.paralino.core.presentation.groups.GroupDetailViewModel
import app.paralino.core.presentation.groups.GroupInitViewModel
import app.paralino.core.presentation.groups.GroupSettingsViewModel
import app.paralino.core.presentation.invite.input.MemberInviteViewModel
import app.paralino.core.presentation.map.FocusedMarkerData
import app.paralino.core.presentation.map.MapHomeScreenEvent
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapHomeScreenUiEvent
import app.paralino.core.presentation.map.MapHomeViewModel
import app.paralino.core.presentation.map.MapSheetSelectedContentType
import app.paralino.core.presentation.map.asFocusedMarker
import app.paralino.core.presentation.map.asReady
import app.paralino.core.presentation.places.create.CreatePlaceViewModel
import app.paralino.core.presentation.places.detail.PlaceDetailScreenState
import app.paralino.core.presentation.places.detail.PlaceDetailViewModel
import app.paralino.core.presentation.places.list.PlaceListItemData
import app.paralino.core.presentation.places.list.PlacesListScreenState
import app.paralino.core.presentation.places.list.PlacesViewModel
import app.paralino.core.presentation.settings.billing.PlanInfoViewModel
import app.paralino.core.presentation.share.SharingStartViewModel
import app.paralino.core.ui.components.BottomSheetPickerType
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.groups.GroupCreateScreenCore
import app.paralino.core.ui.groups.GroupDetailScreenCore
import app.paralino.core.ui.groups.GroupSettingsScreenCore
import app.paralino.core.ui.invite.MemberInviteScreenCore
import app.paralino.core.ui.login.GroupInitScreenCore
import app.paralino.core.ui.map.BottomSheetPosition
import app.paralino.core.ui.map.MainMapSheet
import app.paralino.core.ui.map.MapHomeScreenElements
import app.paralino.core.ui.onboarding.FreePlanInfoScreenCore
import app.paralino.core.ui.places.CreatePlaceScreenCore
import app.paralino.core.ui.places.PlaceDetailScreenCore
import app.paralino.core.ui.places.PlacesScreenCore
import app.paralino.core.ui.share.SharingStartScreenCore
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.collectLatest
import platform.UIKit.UIView

object HomeScreens {
    fun GroupInitScreenVC(
        vm: GroupInitViewModel,
        openSettings: () -> Unit,
        onCreateGroup: () -> Unit,
        openFreePlanInfo: () -> Unit,
        openPersonalCodes: () -> Unit,
        onSharePersonalCode: (String) -> Unit,
        askNotificationPermission: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            GroupInitScreenCore(
                appState = it,
                vm = vm,
                openSettings = openSettings,
                onCreateGroup = onCreateGroup,
                openFreePlanInfo = openFreePlanInfo,
                openPersonalCodes = openPersonalCodes,
                onSharePersonalCode = onSharePersonalCode,
                askNotificationPermission = askNotificationPermission,
            )
        }
    }

    fun GroupCreateScreenVC(
        vm: CreateGroupViewModel,
        onBottomSheetAction: (BottomSheetPickerType.EmojiPickerBottomSheet) -> Unit,
        groupUpdated: (String) -> Unit
    ) = ComposeUIViewController {
        RootScreenIOS {
            GroupCreateScreenCore(
                appState = it,
                vm = vm,
                groupUpdated = groupUpdated,
                onBottomSheetAction = onBottomSheetAction,
            )
        }
    }

    fun GroupDetailScreenVC(
        vm: GroupDetailViewModel,
        groupId: String?,
        openMemberAdd: () -> Unit,
        openMap: () -> Unit,
        openPlaces: () -> Unit,
        openGroupSettings: () -> Unit,
        openMemberRawData: (String) -> Unit,
        openSharing: () -> Unit,
        openPaywall: () -> Unit,
        onNavigateBack: () -> Unit,
        askNotificationPermission: () -> Unit,
        askLocationService: () -> Unit,
        askLocationPermission: () -> Unit,
        askBackgroundLocationPermission: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            GroupDetailScreenCore(
                appState = it,
                vm = vm,
                groupId = groupId,
                contentPadding = PaddingValues(bottom = 30.dp),
                openMemberAdd = openMemberAdd,
                openMap = openMap,
                openPlaces = openPlaces,
                openGroupSettings = openGroupSettings,
                openMemberRawData = openMemberRawData,
                openSharing = openSharing,
                openPaywall = openPaywall,
                onNavigateBack = onNavigateBack,
                askNotificationPermission = askNotificationPermission,
                askLocationService = askLocationService,
                askLocationPermission = askLocationPermission,
                askBackgroundLocationPermission = askBackgroundLocationPermission
            )
        }
    }

    fun GroupSettingsScreenVC(
        vm: GroupSettingsViewModel,
        openEditGroup: () -> Unit,
        onGroupDeleted: () -> Unit,
        openRawDataScreen: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            GroupSettingsScreenCore(
                appState = it,
                vm = vm,
                openEditGroup = openEditGroup,
                onGroupDeleted = onGroupDeleted,
                openRawDataScreen = openRawDataScreen
            )
        }
    }

    fun MemberInviteScreenVC(
        vm: MemberInviteViewModel,
        onPickGroup: (BottomSheetPickerType.GroupPickerBottomSheet) -> Unit,
        onMemberAdded: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            MemberInviteScreenCore(
                appState = it,
                vm = vm,
                contentPadding = PaddingValues(),
                onPickGroup = onPickGroup,
                onMemberAdded = onMemberAdded
            )
        }
    }

    fun StartSharingScreenVC(
        vm: SharingStartViewModel,
        groupId: String?,
        onPickGroup: (BottomSheetPickerType.GroupPickerBottomSheet) -> Unit,
        onSharingStarted: () -> Unit,
        openSubscriptions: () -> Unit,
        askNotificationPermission: () -> Unit,
        askLocationService: () -> Unit,
        askLocationPermission: () -> Unit,
        askBackgroundLocationPermission: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            SharingStartScreenCore(
                appState = it,
                vm = vm,
                groupId = groupId,
                onPickGroup = onPickGroup,
                onSharingStarted = onSharingStarted,
                openSubscriptions = openSubscriptions,
                askNotificationPermission = askNotificationPermission,
                askLocationService = askLocationService,
                askLocationPermission = askLocationPermission,
                askBackgroundLocationPermission = askBackgroundLocationPermission
            )
        }
    }

    @OptIn(ExperimentalForeignApi::class, ExperimentalMaterialApi::class)
    fun MapSheet(
        vm: MapHomeViewModel,
        initialUiState: MapHomeScreenState,
        trackMarker: (FocusedMarkerData) -> Unit,
        onOpenPlacesScreen: () -> Unit,
        onOpenCreatePlaceScreen: () -> Unit,
        onSheetOffsetChange: (Float) -> Unit,
        onGroupPickerClick: () -> Unit,
        onGroupDetailsClick: () -> Unit,
        onSharingStartClick: () -> Unit,
        onSettingsClick: () -> Unit,
        onPlaceClick: (PlaceListItemData) -> Unit,
        onCreateGroup: () -> Unit,
        openPaywall: () -> Unit,
        mapUIView: () -> UIView,
    ) = ComposeUIViewController() {
        RootScreenIOS {
            MessageDispatcherEffect(vm, it)

            val uiState by vm.uiState.collectAsState(initialUiState)
            val readyState = rememberUpdatedState(uiState.asReady()).value

            BoxWithConstraints {
                val height = this.maxHeight
                val density = LocalDensity.current
                val sheetState = rememberSwipeableState(
                    initialValue = if (readyState?.mapHomeScreenFocusType?.isMemberOrPlaceFocused != false) {
                        BottomSheetPosition.HIDDEN
                    } else {
                        BottomSheetPosition.HIDDEN
                    },
                    confirmStateChange = {
                        it != BottomSheetPosition.HIDDEN
                    }
                )

                val bottomOffset = density.run {
                    try {
                        sheetState.offset.value.toDp()
                    } catch (e: Exception) {
                        0.dp
                    }
                }

                val collapsedAnchorBottomPaddingPixels = with(LocalDensity.current) {
                    15.dp.toPx()
                }

                LaunchedEffect(key1 = Unit) {
                    vm.events.collectLatest { event ->
                        when (event) {
                            MapHomeScreenEvent.AddGroup -> {
                                onCreateGroup()
                            }

                            MapHomeScreenEvent.GroupsPlanLimitReached -> {
                                openPaywall()
                            }

                            null -> {}
                        }
                    }
                }

                LaunchedEffect(sheetState.currentValue) {
                    snapshotFlow { sheetState.currentValue }.collectLatest {
                        val bottomOffset = (height - bottomOffset).coerceIn(
                            0.dp,
                            height * BottomSheetPosition.HALF_EXPANDED.screenHeightPercent
                        )
                        onSheetOffsetChange(bottomOffset.value)
                    }
                }

                val dialog = uiState.alertDialog
                dialog?.Show {
                    vm.onEvent(MapHomeScreenUiEvent.DialogAction(AlertDialogAction(dialog, it)))
                }

                MainMapSheet(
                    appState = it,
                    swipeableState = sheetState,
                    trackMarker = trackMarker,
                    readyState = readyState,
                    createAnchors = {
                        BottomSheetPosition.swipeAnchors(
                            screenHeightPixels = it,
                            collapsedAnchorBottomPaddingPixels = collapsedAnchorBottomPaddingPixels
                        )
                    },
                    onOpenPlacesScreen = onOpenPlacesScreen,
                    onOpenCreatePlaceScreen = onOpenCreatePlaceScreen,
                    onOpenGroupScreen = onGroupDetailsClick,
                    onFocusOnPlacesClick = {
                        vm.setSelectedContentType(MapSheetSelectedContentType.PLACES)
                    },
                    onFocusOnMembersClick = {
                        vm.setSelectedContentType(MapSheetSelectedContentType.MEMBERS)
                    },
                    screenContent = {
                        UIKitView(
                            factory = mapUIView,
                            modifier = Modifier.fillMaxSize(),
                        )

                        MapHomeScreenElements(
                            modifier = Modifier.padding(top = 50.dp),
                            state = uiState,
                            sheetPeekHeight = (this.maxHeight - bottomOffset)
                                .plus(if (sheetState.currentValue == BottomSheetPosition.HIDDEN) 35.dp else 0.dp)
                                .coerceAtMost(this.maxHeight * BottomSheetPosition.HALF_EXPANDED.screenHeightPercent),
                            onGroupPickerClick = onGroupPickerClick,
                            onGroupDetailsClick = onGroupDetailsClick,
                            onSharingStartClick = onSharingStartClick,
                            onSettingsClick = onSettingsClick,
                            onPlaceClick = onPlaceClick,
                            onMemberActionClick = { action, state ->
                                vm.memberAction(action, state)
                            },
                            onMemberClick = {
                                // TODO refocus right away
                                vm.trackMarker(it.asFocusedMarker())
                            },
                            onToggleLocation = {
                                vm.toggleMarkerVisibility(it)
                            },
                            onRequestLocation = {
                                vm.requestLocationUpdate(it)
                            },
                            onMapModeSwitch = {
                                vm.switchMapMode()
                            },
                            onCloseFocusedItem = {
                                vm.hideFocusedItem()
                            }
                        )
                    }
                )
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    fun PlacesScreenVC(
        vm: PlacesViewModel,
        addNewPlace: () -> Unit,
        openPaywall: () -> Unit,
        openPlaceDetailScreen: (place: PlaceListItemData) -> Unit,
        askNotificationPermission: () -> Unit,
        mapUIView: (PlacesListScreenState) -> UIView,
    ) = ComposeUIViewController {
        RootScreenIOS {

            var userScroll by remember {
                mutableStateOf(true)
            }

            PlacesScreenCore(
                appState = it,
                vm = vm,
                userScrollEnabled = userScroll,
                contentPadding = PaddingValues(bottom = 30.dp),
                askNotificationPermission = askNotificationPermission,
                addNewPlace = addNewPlace,
                openPaywall = openPaywall,
                openPlaceDetails = openPlaceDetailScreen,
                mapContent = { state ->
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        UIKitView(
                            factory = { mapUIView(state) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            )
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    fun PlaceDetailScreenVC(
        vm: PlaceDetailViewModel,
        openPlaceRawDataScreen: () -> Unit,
        openPlaceEditScreen: () -> Unit,
        onNavigateBack: () -> Unit,
        mapUIView: (PlaceDetailScreenState) -> UIView,
    ) = ComposeUIViewController {
        RootScreenIOS {

            var userScroll by remember {
                mutableStateOf(true)
            }

            PlaceDetailScreenCore(
                appState = it,
                vm = vm,
                userScrollEnabled = userScroll,
                contentPadding = PaddingValues(bottom = 30.dp),
                onNavigateBack = onNavigateBack,
                openPlaceRawDataScreen = openPlaceRawDataScreen,
                openPlaceEditScreen = openPlaceEditScreen,
                mapContent = { state ->
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        UIKitView(
                            factory = { mapUIView(state) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            )
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    fun PlaceCreateScreenVC(
        vm: CreatePlaceViewModel,
        onNavigateBack: () -> Unit,
        onBottomSheetAction: (BottomSheetPickerType.EmojiPickerBottomSheet) -> Unit,
        mapUIView: () -> UIView,
    ) = ComposeUIViewController {
        RootScreenIOS {
            CreatePlaceScreenCore(
                appState = it,
                vm = vm,
                onNavigateBack = onNavigateBack,
                onBottomSheetAction = onBottomSheetAction,
                mapContent = {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        UIKitView(
                            factory = { mapUIView() },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            )
        }
    }

    fun FreePlanInfoScreenVC(
        vm: PlanInfoViewModel,
        showBanner: Boolean,
        onContinue: () -> Unit,
        openPaywall: () -> Unit
    ) = ComposeUIViewController {
        RootScreenIOS {
            FreePlanInfoScreenCore(
                coreUiState = it,
                vm = vm,
                showBanner = showBanner,
                onContinue = onContinue,
                openPaywall = openPaywall,
            )
        }
    }
}
