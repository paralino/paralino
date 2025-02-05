package app.paralino.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitViewController
import androidx.compose.ui.window.ComposeUIViewController
import app.paralino.core.presentation.groups.detail.GroupDetailViewModel
import app.paralino.core.presentation.groups.edit.CreateGroupViewModel
import app.paralino.core.presentation.groups.edit.GroupInitViewModel
import app.paralino.core.presentation.groups.settings.BatteryNotificationsViewModel
import app.paralino.core.presentation.groups.settings.GroupSettingsViewModel
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
import app.paralino.core.ui.groups.BatteryNotificationsScreenCore
import app.paralino.core.ui.groups.GroupCreateScreenCore
import app.paralino.core.ui.groups.GroupDetailScreenCore
import app.paralino.core.ui.groups.GroupSettingsScreenCore
import app.paralino.core.ui.invite.MemberInviteScreenCore
import app.paralino.core.ui.map.BottomSheetPosition
import app.paralino.core.ui.map.MainMapSheet
import app.paralino.core.ui.map.MapHomeScreenElements
import app.paralino.core.ui.map.rememberSwipeableSheetState
import app.paralino.core.ui.onboarding.FreePlanInfoScreenCore
import app.paralino.core.ui.onboarding.account.GroupInitScreenCore
import app.paralino.core.ui.places.CreatePlaceScreenCore
import app.paralino.core.ui.places.PlaceAlertsSettingsScreenCore
import app.paralino.core.ui.places.PlaceDetailScreenCore
import app.paralino.core.ui.places.PlacesScreenCore
import app.paralino.core.ui.share.SharingStartScreenCore
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import platform.UIKit.UIViewController

object HomeScreens {
    fun GroupInitScreenVC(
        vm: GroupInitViewModel,
        paddingValues: AppleScreenPaddingValues,
        openSettings: () -> Unit,
        onCreateGroup: () -> Unit,
        openFreePlanInfo: () -> Unit,
        openPersonalCodes: () -> Unit,
        openPaywall: () -> Unit,
        onSharePersonalCode: (String) -> Unit,
        askNotificationPermission: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            GroupInitScreenCore(
                appState = it,
                vm = vm,
                paddingValues = paddingValues.toComposePaddingValues(),
                openSettings = openSettings,
                onCreateGroup = onCreateGroup,
                openFreePlanInfo = openFreePlanInfo,
                openPersonalCodes = openPersonalCodes,
                openPaywall = openPaywall,
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
        paddingValues: AppleScreenPaddingValues,
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
                contentPadding = paddingValues.toComposePaddingValues(),
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
        openBatteryNotificationsSettings: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            GroupSettingsScreenCore(
                appState = it,
                vm = vm,
                openEditGroup = openEditGroup,
                onGroupDeleted = onGroupDeleted,
                openRawDataScreen = openRawDataScreen,
                openBatteryNotificationsSettings = openBatteryNotificationsSettings
            )
        }
    }

    fun BatteryNotificationsSettingsScreenVC(
        vm: BatteryNotificationsViewModel,
        paddingValues: AppleScreenPaddingValues,
        openPaywall: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            BatteryNotificationsScreenCore(
                appState = it,
                vm = vm,
                contentPadding = paddingValues.toComposePaddingValues(),
                openPaywall = openPaywall
            )
        }
    }

    fun MemberInviteScreenVC(
        vm: MemberInviteViewModel,
        isOnboarding: Boolean,
        onPickGroup: (BottomSheetPickerType.GroupPickerBottomSheet) -> Unit,
        onMemberAdded: () -> Unit,
        onSkipOnboarding: () -> Unit
    ) = ComposeUIViewController {
        RootScreenIOS {
            MemberInviteScreenCore(
                appState = it,
                vm = vm,
                isOnboarding = isOnboarding,
                contentPadding = PaddingValues(),
                onPickGroup = onPickGroup,
                onMemberAdded = onMemberAdded,
                onSkipOnboarding = onSkipOnboarding
            )
        }
    }

    fun StartSharingScreenVC(
        vm: SharingStartViewModel,
        onPickGroup: (BottomSheetPickerType.GroupPickerBottomSheet) -> Unit,
        onSharingStarted: () -> Unit,
        onSkipOnboarding: () -> Unit,
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
                onPickGroup = onPickGroup,
                onSharingStarted = onSharingStarted,
                onSkipOnboarding = onSkipOnboarding,
                openSubscriptions = openSubscriptions,
                askNotificationPermission = askNotificationPermission,
                askLocationService = askLocationService,
                askLocationPermission = askLocationPermission,
                askBackgroundLocationPermission = askBackgroundLocationPermission
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    fun MapSheet(
        vm: MapHomeViewModel,
        paddingValues: AppleScreenPaddingValues,
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
        openFreePlanInfo: () -> Unit,
        openPaywall: () -> Unit,
        mapUIView: () -> UIViewController,
    ) = ComposeUIViewController() {
        RootScreenIOS {
            MessageDispatcherEffect(vm, it)

            val uiState by vm.uiState.collectAsState(initialUiState)
            val readyState = rememberUpdatedState(uiState.asReady()).value

            BoxWithConstraints {
                val height = this.maxHeight
                val density = LocalDensity.current

                val collapsedAnchorBottomPaddingPixels = with(LocalDensity.current) {
                    15.dp.toPx()
                }

                val sheetState = rememberSwipeableSheetState(
                    initialValue = if (readyState?.mapHomeScreenFocusType?.isMemberOrPlaceFocused != false) {
                        BottomSheetPosition.HIDDEN
                    } else {
                        BottomSheetPosition.HIDDEN
                    },
                    collapsedAnchorBottomPaddingPixels = collapsedAnchorBottomPaddingPixels,
                    confirmValueChange = {
                        it != BottomSheetPosition.HIDDEN
                    }
                )

                val bottomOffset = density.run {
                    try {
                        sheetState.offset.toDp()
                    } catch (e: Exception) {
                        0.dp
                    }
                }

                LaunchedEffect(key1 = Unit) {
                    vm.events.collectLatest { event ->
                        when (event) {
                            MapHomeScreenEvent.AddGroup -> onCreateGroup()
                            MapHomeScreenEvent.ShowFreePlanInfo -> openFreePlanInfo()
                            MapHomeScreenEvent.OpenPaywall -> openPaywall()
                            null -> {}
                        }
                    }
                }

                LaunchedEffect(bottomOffset) {
                    snapshotFlow { bottomOffset }
                        .distinctUntilChanged()
                        .collectLatest {
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
                        UIKitViewController(
                            modifier = Modifier.fillMaxSize(),
                            factory = mapUIView,
                            properties = UIKitInteropProperties(
                                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                                isNativeAccessibilityEnabled = true
                            )
                        )

                        MapHomeScreenElements(
                            modifier = Modifier.padding(top = paddingValues.top.dp),
                            state = uiState,
                            sheetPeekHeight = (this.maxHeight - bottomOffset).coerceIn(
                                minimumValue = if (sheetState.currentValue == BottomSheetPosition.HIDDEN) paddingValues.bottom.dp else 0.dp,
                                maximumValue = this.maxHeight * BottomSheetPosition.HALF_EXPANDED.screenHeightPercent
                            ),
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

    @OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)
    fun PlacesScreenVC(
        vm: PlacesViewModel,
        paddingValues: AppleScreenPaddingValues,
        addNewPlace: () -> Unit,
        openPaywall: () -> Unit,
        onSkipOnboarding: () -> Unit,
        openPlaceDetailScreen: (place: PlaceListItemData) -> Unit,
        askNotificationPermission: () -> Unit,
        mapUIView: (PlacesListScreenState) -> UIViewController,
    ) = ComposeUIViewController {
        RootScreenIOS {

            var userScroll by remember {
                mutableStateOf(true)
            }

            PlacesScreenCore(
                appState = it,
                vm = vm,
                userScrollEnabled = userScroll,
                contentPadding = paddingValues.toComposePaddingValues(),
                askNotificationPermission = askNotificationPermission,
                addNewPlace = addNewPlace,
                openPaywall = openPaywall,
                onSkipOnboarding = onSkipOnboarding,
                openPlaceDetails = openPlaceDetailScreen,
                mapContent = { state ->
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        UIKitViewController(
                            modifier = Modifier.fillMaxSize(),
                            factory = { mapUIView(state) },
                            properties = UIKitInteropProperties(
                                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                                isNativeAccessibilityEnabled = true
                            )
                        )
                    }
                }
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    fun PlaceDetailScreenVC(
        vm: PlaceDetailViewModel,
        paddingValues: AppleScreenPaddingValues,
        openPlaceRawDataScreen: () -> Unit,
        openPlaceAlertsScreen: () -> Unit,
        openPlaceEditScreen: () -> Unit,
        onNavigateBack: () -> Unit,
        mapUIView: (PlaceDetailScreenState) -> UIViewController,
    ) = ComposeUIViewController {
        RootScreenIOS {

            var userScroll by remember {
                mutableStateOf(true)
            }

            PlaceDetailScreenCore(
                appState = it,
                vm = vm,
                userScrollEnabled = userScroll,
                contentPadding = paddingValues.toComposePaddingValues(),
                onNavigateBack = onNavigateBack,
                openPlaceRawDataScreen = openPlaceRawDataScreen,
                openPlaceAlertsScreen = openPlaceAlertsScreen,
                openPlaceEditScreen = openPlaceEditScreen,
                mapContent = { state ->
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        UIKitViewController(
                            modifier = Modifier.fillMaxSize(),
                            factory = { mapUIView(state) },
                            properties = UIKitInteropProperties(
                                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                                isNativeAccessibilityEnabled = true
                            )
                        )
                    }
                }
            )
        }
    }

    fun PlaceAlertsSettingsScreenVC(
        vm: PlaceDetailViewModel,
        paddingValues: AppleScreenPaddingValues,
    ) = ComposeUIViewController {
        RootScreenIOS {
            PlaceAlertsSettingsScreenCore(
                appState = it,
                vm = vm,
                contentPadding = paddingValues.toComposePaddingValues()
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    fun PlaceCreateScreenVC(
        vm: CreatePlaceViewModel,
        onNavigateBack: () -> Unit,
        onBottomSheetAction: (BottomSheetPickerType.EmojiPickerBottomSheet) -> Unit,
        mapUIView: () -> UIViewController,
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
                        UIKitViewController(
                            modifier = Modifier.fillMaxSize(),
                            factory = mapUIView,
                            properties = UIKitInteropProperties(
                                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                                isNativeAccessibilityEnabled = true
                            )
                        )
                    }
                }
            )
        }
    }

    fun FreePlanInfoScreenVC(
        vm: PlanInfoViewModel,
        paddingValues: AppleScreenPaddingValues,
        onContinue: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            FreePlanInfoScreenCore(
                coreUiState = it,
                vm = vm,
                paddingValues = paddingValues.toComposePaddingValues(),
                onContinue = onContinue,
            )
        }
    }
}
