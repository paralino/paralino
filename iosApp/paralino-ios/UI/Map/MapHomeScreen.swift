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
                ScreensIOS.shared.map.MapSheet(
                    vm: mapState.vm,
                    paddingValues: safeAreaInsets,
                    onOpenPlacesScreen: { groupId in
                        path.append(.home(.places(groupId: groupId, isOnboarding: false)))
                    },
                    onOpenCreatePlaceScreen: { groupId, latLng in
                        path.append(.home(.placeCreate(groupId: groupId, placeId: nil, lat: latLng?.lat, lng: latLng?.lng)))
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
                    onHistorySettingsClick: { groupId in
                        path.append(.home(.locationHistorySettings(groupId: GroupOrSpaceIdHelper.shared.unwrap(id: groupId)!)))
                    },
                    onMemberInviteClick: { groupId in
                        path.append(.home(.memberInvite(groupId: groupId, code: nil, isOnboarding: false)))
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
                    openPaywall: { type in
                        path.append(.settings(.paywall(type: type)))
                    },
                    openPersonalSpace: {
                        path.append(.home(.personalSpace))
                    },
                    openPersonalCodes: {
                        path.append(.settings(.personalCodes))
                    },
                    openRawDataScreen: { type in
                        path.append(.settings(.rawData(type: type)))
                    },
                    onSharePersonalCode: { content in
                        shareText = ShareText(text: content)
                    },
                    askNotificationPermission: {
                        appDelegate.notificationPermissionHandler.requestNotificationPermission(
                            messageDispather: mapState.vm.messageDispatcher
                        )
                    },
                    askLocationService: {
                        appDelegate.locationPermissionHandler.requestLocationServices(
                            messageDispather: mapState.vm.messageDispatcher
                        )
                    },
                    askLocationPermission: {
                        appDelegate.locationPermissionHandler.requestLocationPermission(
                            messageDispather: mapState.vm.messageDispatcher
                        )
                    },
                    askBackgroundLocationPermission: {
                        appDelegate.locationPermissionHandler.requestLocationPermission(
                            messageDispather: mapState.vm.messageDispatcher
                        )
                    },
                    mapUIView: { () -> UIViewController in
                        let controller = MapComponentVC()
                        controller.additionalSafeAreaInsets = .init(top: 65, left: 0, bottom: 0, right: 0)
                        controller.mapState = mapState
                        controller.onMapPan = { mapState.onMapTouch() }
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
            mapState.onMainEvent(event: MapHomeScreenUiEventSwitchViewSource(viewSource: MapScreenViewSourceTypeDefaultGroupOrSpace(id: nil)))
        }
        .onDisappear {
            Logr.d(tag:"home_screen", message: "Home screen disappear")
            mapState.onMainEvent(event: MapHomeScreenUiEventUiLifecycleChange(active: false))
        }
        .onChange(of: scenePhase, initial: true) { oldScene, newScene in
            Logr.d(tag:"home_screen", message: "Home screen scene switch (old: \(oldScene)) - (new: \(newScene))")
            
            switch newScene {
            case .active:
                mapState.onMainEvent(event: MapHomeScreenUiEventUiLifecycleChange(active: true))
            case .inactive:
                mapState.onMainEvent(event: MapHomeScreenUiEventUiLifecycleChange(active: false))
            case .background:
                mapState.onMainEvent(event: MapHomeScreenUiEventUiLifecycleChange(active: false))
            @unknown default:
                break
            }
        }
        .groupPickerSheet(
            $groupPicker,
            onSelectGroup: { state in
                groupPicker = nil
                mapState.onMainEvent(
                    event: MapHomeScreenUiEventSwitchViewSource(viewSource: MapScreenViewSourceTypeDefaultGroupOrSpace(id: state.id))
                )
            },
            onEditGroup: { state in
                groupPicker = nil
                mapState.onMainEvent(event: MapHomeScreenUiEventOpenGroupDetails(groupId: state.id))
            },
            onCreateGroup: {
                groupPicker = nil
                mapState.onMainEvent(event: MapHomeScreenUiEventTryCreateGroup())
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
