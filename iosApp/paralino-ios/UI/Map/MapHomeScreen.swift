//
//  MapHomeScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 17.06.23.
//

import SwiftUI
import shared
import MapKit

struct MapHomeScreen: View {

    @EnvironmentObject var appDelegate: AppDelegate
    @Binding var path: [NavigationGraph]
    @StateObject var mapState: MapHomeState = MapHomeState()
    @State var groupPicker: BottomSheetPickerTypeGroupPickerBottomSheet? = nil
    @State var shareText: ShareText?
    
    @Environment(\.safeAreaInsets) private var safeAreaInsets
    @Environment(\.scenePhase) var scenePhase
    
    var body: some View {
        ZStack {
            ComposeRepresentable {
                ScreensIOS.shared.home.MapSheet(
                    vm: mapState.vm,
                    paddingValues: safeAreaInsets,
                    initialUiState: mapState.uiState,
                    onOpenPlacesScreen: { groupId in
                        path.append(.home(.places(groupId: groupId, isOnboarding: false)))
                    },
                    onOpenCreatePlaceScreen: { groupId in
                        path.append(.home(.placeCreate(groupId: groupId, placeId: nil, lat: nil, lng: nil)))
                    },
                    onSheetOffsetChange: { offset in
                        mapState.bottomSafeArea = max(CGFloat(offset.floatValue - safeAreaInsets.bottom), 0)
                    },
                    onGroupPickerClick: { showPersonalSpace in
                        groupPicker = mapState.vm.getGroupPickerState(showPersonalSpace: showPersonalSpace.boolValue)
                    },
                    onGroupDetailsClick: { groupId in
                        path.append(.home(.groupDetail(groupId: groupId)))
                    },
                    onSharingStartClick: { groupId in
                        path.append(.home(.sharingStart(groupId: groupId, isOnboarding: false)))
                    },
                    onSettingsClick: {
                        path.append(.settings(.settings))
                    },
                    onPlaceClick: { event in
                        path.append(.home(.placeDetail(groupId: event.groupId, placeId: event.placeId)))
                    },
                    onDirectionsRequest: { event in
                        let url = event.navigationApp.buildLaunchURL(lat: event.lat, lng: event.lng, title: event.name)
                        if let deeplink = URL(string: url), UIApplication.shared.canOpenURL(deeplink) {
                            UIApplication.shared.open(deeplink)
                        }
                    },
                    onCreateGroup: {
                        path.append(.home(.groupCreate(groupId: nil)))
                    },
                    openPaywall: {
                        path.append(.settings(.paywall(type: BillingScreenSourceType.upgrade)))
                    },
                    openPersonalSpace: {
                        path.append(.home(.personalSpace))
                    },
                    openPersonalCodes: {
                        path.append(.settings(.personalCodes))
                    },
                    onSharePersonalCode: { content in
                        shareText = ShareText(text: content)
                    },
                    askNotificationPermission: {
                        appDelegate.notificationPermissionHandler.requestNotificationPermission(
                            messageDispather: mapState.vm.messageDispatcher
                        )
                    },
                    openLocationHistory: { event in
                        path.append(.home(.locationHistory(groupId: event.groupId, memberUserId: event.memberUserId, deviceId: event.deviceId)))
                    },
                    mapUIView: { () -> UIViewController in
                        let controller = MapComponentVC()
                        controller.additionalSafeAreaInsets = .init(top: 65, left: 0, bottom: 0, right: 0)
                        controller.mapState = mapState
                        controller.onMapPan = {
                            mapState.vm.onEvent(event: MapHomeScreenUiEventOnMapTouch())
                        }
                        controller.onMapLongPress = { point in
                            if let readyState = mapState.uiState.asReady() {
                                path.append(.home(.placeCreate(groupId: GroupOrSpaceIdHelper.shared.unwrap(id: readyState.groupOrSpaceId)!, placeId: nil, lat: point.latitude, lng: point.longitude)))
                            }
                        }
                        return controller
                    }
                )
            }
            .ignoresSafeArea(.all, edges: .vertical)
            .toolbar(.hidden, for: .navigationBar)
        }
        .onAppear {
            Logr.d(tag:"home_screen", message: "Home screen appear")
            mapState.vm.onEvent(event: MapHomeScreenUiEventSwitchViewSource(viewSource: MapScreenViewSourceTypeDefaultGroupOrSpace(id: nil)))
        }
        .onDisappear {
            Logr.d(tag:"home_screen", message: "Home screen disappear")
            mapState.vm.onEvent(event: MapHomeScreenUiEventUiLifecycleChange(active: false))
        }
        .onChange(of: scenePhase, initial: true) { oldScene, newScene in
            Logr.d(tag:"home_screen", message: "Home screen scene switch (old: \(oldScene)) - (new: \(newScene))")
            
            switch newScene {
            case .active:
                mapState.vm.onEvent(event: MapHomeScreenUiEventUiLifecycleChange(active: true))
            case .inactive:
                mapState.vm.onEvent(event: MapHomeScreenUiEventUiLifecycleChange(active: false))
            case .background:
                mapState.vm.onEvent(event: MapHomeScreenUiEventUiLifecycleChange(active: false))
            @unknown default:
                break
            }
        }
        .groupPickerSheet(
            $groupPicker,
            onSelectGroup: { state in
                groupPicker = nil
                mapState.vm.onEvent(
                    event: MapHomeScreenUiEventSwitchViewSource(viewSource: MapScreenViewSourceTypeDefaultGroupOrSpace(id: state.id))
                )
            },
            onEditGroup: { state in
                groupPicker = nil
                mapState.vm.onEvent(event: MapHomeScreenUiEventOpenGroupDetails(groupId: state.id))
            },
            onCreateGroup: {
                groupPicker = nil
                mapState.vm.onEvent(event: MapHomeScreenUiEventTryCreateGroup())
            },
            onJoinGroup: {
                groupPicker = nil
                path.append(.settings(.personalCodes))
            },
            onOpenSubscription: {
                groupPicker = nil
                path.append(.settings(.paywall(groupId: nil, type: .upgrade)))
            }
        )
        .sheet(item: $shareText) { shareText in
            ActivityView(text: shareText.text)
        }
        .navigationBarBackButtonHidden(true)
    }
}

//struct MapHomeScreen_Previews: PreviewProvider {
//    @State static var path: [NavigationGraph] = [.home(.map)]
//    @StateObject var mapState = MapHomeState()
//    static var previews: some View {
//        MapHomeScreen(path: $path, mapState: mapState)
//    }
//}
