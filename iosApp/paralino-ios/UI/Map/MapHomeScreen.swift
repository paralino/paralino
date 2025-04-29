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
    
    @Binding var path: [NavigationGraph]
    @StateObject var mapState: MapHomeState = MapHomeState()
    @State var groupPicker: BottomSheetPickerTypeGroupPickerBottomSheet? = nil
    
    @Environment(\.safeAreaInsets) private var safeAreaInsets
    @Environment(\.scenePhase) var scenePhase
    
    var body: some View {
        ZStack {
            if mapState.uiState is MapHomeScreenState.Ready {
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
                        onGroupPickerClick: {
                            groupPicker = mapState.vm.getGroupPickerState()
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
                        openFreePlanInfo: {
                            path.append(.home(.freePlanInfo))
                        },
                        openPaywall: {
                            path.append(.settings(.paywall(type: BillingScreenSourceType.upgrade)))
                        },
                        mapUIView: { () -> UIViewController in
                            let controller = MapComponentVC()
                            controller.additionalSafeAreaInsets = .init(top: 60, left: 0, bottom: 0, right: 0)
                            controller.mapState = mapState
                            controller.onMapPan = {
                                mapState.vm.onEvent(event: MapHomeScreenUiEventOnMapTouch())
                            }
                            controller.onMapLongPress = { point in
                                if let readyState = mapState.uiState.asReady() {
                                    path.append(.home(.placeCreate(groupId: readyState.groupId, placeId: nil, lat: point.latitude, lng: point.longitude)))
                                }
                            }
                            return controller
                        }
                    )
                }
                .ignoresSafeArea(.all, edges: .vertical)
                .toolbar(.hidden, for: .navigationBar)
            } else if mapState.uiState is MapHomeScreenState.Empty {
                VStack {
                    Image("paralino_logo")
                        .resizable()
                        .scaledToFit()
                        .colorMultiply(.themePrimary)
                        .frame(width: 100, height: 100)
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle())
                }
            } else {
                VStack {
                    Image("paralino_logo")
                        .resizable()
                        .scaledToFit()
                        .colorMultiply(.themePrimary)
                        .frame(width: 60, height: 60)
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle())
                }
            }
        }
        .onAppear {
            Logr.d(tag:"home_screen", message: "Home screen appear")
            mapState.vm.switchGroup(groupId: "")
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
                mapState.vm.switchGroup(groupId: state.id)
            },
            onEditGroup: { state in
                groupPicker = nil
                path.append(.home(.groupDetail(groupId: state.id)))
            },
            onCreateGroup: {
                groupPicker = nil
                mapState.vm.tryCreateGroup()
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
