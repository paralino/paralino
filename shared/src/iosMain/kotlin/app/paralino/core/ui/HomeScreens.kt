package app.paralino.core.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberSwipeableState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import app.paralino.core.presentation.groups.CreateGroupViewModel
import app.paralino.core.presentation.groups.GroupDetailViewModel
import app.paralino.core.presentation.groups.GroupInitViewModel
import app.paralino.core.presentation.invite.input.MemberInviteViewModel
import app.paralino.core.presentation.map.FocusedMarkerData
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapHomeViewModel
import app.paralino.core.presentation.map.asFocusedMarker
import app.paralino.core.presentation.map.asReady
import app.paralino.core.presentation.places.list.PlaceListItemData
import app.paralino.core.presentation.share.SharingStartViewModel
import app.paralino.core.ui.components.BottomSheetsScreen
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.groups.GroupCreateScreenCore
import app.paralino.core.ui.groups.GroupDetailScreenCore
import app.paralino.core.ui.invite.MemberInviteScreenCore
import app.paralino.core.ui.login.GroupInitScreenCore
import app.paralino.core.ui.map.BottomSheetPosition
import app.paralino.core.ui.map.MainMapSheet
import app.paralino.core.ui.map.MapHomeScreenElements
import app.paralino.core.ui.share.SharingStartScreenCore
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.collectLatest
import platform.UIKit.UIView

object HomeScreens {
    fun GroupInitScreenVC(
        vm: GroupInitViewModel,
        onShowDialog: (MainAlertDialog?) -> Unit,
        openSettings: () -> Unit,
        onCreateGroup: () -> Unit,
        openPersonalCodes: () -> Unit,
        onSharePersonalCode: (String) -> Unit,
        askNotificationPermission: () -> Unit,
        onClose: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            GroupInitScreenCore(
                appState = it,
                vm = vm,
                onShowDialog = onShowDialog,
                openSettings = openSettings,
                onCreateGroup = onCreateGroup,
                openPersonalCodes = openPersonalCodes,
                onSharePersonalCode = onSharePersonalCode,
                askNotificationPermission = askNotificationPermission,
                onClose = onClose
            )
        }
    }

    fun GroupCreateScreenVC(
        vm: CreateGroupViewModel,
        onBottomSheetAction: (BottomSheetsScreen) -> Unit,
        groupUpdated: (String) -> Unit,
        onExit: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            GroupCreateScreenCore(
                appState = it,
                vm = vm,
                groupUpdated = groupUpdated,
                onBottomSheetAction = onBottomSheetAction,
                onExit = onExit,
            )
        }
    }

    fun GroupDetailScreenVC(
        vm: GroupDetailViewModel,
        groupId: String?,
        openMemberAdd: () -> Unit,
        openGroupEdit: () -> Unit,
        openMap: () -> Unit,
        openPlaces: () -> Unit,
        openGroupRawData: () -> Unit,
        openMemberRawData: (String) -> Unit,
        openSharing: () -> Unit,
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
                openMemberAdd = openMemberAdd,
                openGroupEdit = openGroupEdit,
                openMap = openMap,
                openPlaces = openPlaces,
                openGroupRawData = openGroupRawData,
                openMemberRawData = openMemberRawData,
                openSharing = openSharing,
                onNavigateBack = onNavigateBack,
                askNotificationPermission = askNotificationPermission,
                askLocationService = askLocationService,
                askLocationPermission = askLocationPermission,
                askBackgroundLocationPermission = askBackgroundLocationPermission
            )
        }
    }


    fun MemberInviteScreenVC(
        vm: MemberInviteViewModel,
        onPickGroup: (BottomSheetsScreen.GroupPicker) -> Unit,
        onExit: () -> Unit,
    ) = ComposeUIViewController {
        RootScreenIOS {
            MemberInviteScreenCore(
                appState = it, vm = vm, onPickGroup = onPickGroup, onNavigateBack = onExit
            )
        }
    }

    fun StartSharingScreenVC(
        vm: SharingStartViewModel,
        groupId: String?,
        onPickGroup: (BottomSheetsScreen.GroupPicker) -> Unit,
        onNavigateBack: () -> Unit,
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
                onNavigateBack = onNavigateBack,
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

                LaunchedEffect(bottomOffset) {
                    snapshotFlow { bottomOffset }.collectLatest {
                        val bottomOffset = height - bottomOffset
                        onSheetOffsetChange(bottomOffset.value)
                    }
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
                    screenContent = {
                        UIKitView(
                            factory = mapUIView,
                            modifier = Modifier.fillMaxSize(),
                        )

                        MapHomeScreenElements(
                            modifier = Modifier.padding(top = 50.dp),
                            state = uiState,
                            sheetPeekHeight = (this.maxHeight - bottomOffset).coerceAtMost(this.maxHeight * 0.4f),
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
                            onMapModeSwitch = {
                                vm.switchMapMode()
                            },
                            onFocusOnPlacesClick = {
                                vm.toggleFocusOnPlaces()
                            },
                            onFocusOnMembersClick = {
                                vm.toggleFocusOnMembers()
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
}
