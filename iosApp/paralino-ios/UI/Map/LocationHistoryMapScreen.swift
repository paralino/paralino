//
//  LocationHistoryMapScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 2025-06-26.
//

import SwiftUI
import shared
import MapKit

struct LocationHistoryMapScreen: View {
    
    @Binding var path: [NavigationGraph]
    @StateObject var mapState: LocationHistoryMapState
    
    @Environment(\.safeAreaInsets) private var safeAreaInsets
    @Environment(\.scenePhase) var scenePhase
    
    var body: some View {
        ZStack {
            ComposeRepresentable {
                
                ScreensIOS.shared.home.LocationHistoryMapScreen(
                    vm: mapState.vm,
                    paddingValues: safeAreaInsets,
                    onCardHeightChange: { offset in
                        mapState.bottomSafeArea = max(CGFloat(offset.floatValue), 0)
                    },
                    onDirectionsRequest: { event in
                        let url = event.navigationApp.buildLaunchURL(lat: event.lat, lng: event.lng, title: event.name)
                        if let deeplink = URL(string: url), UIApplication.shared.canOpenURL(deeplink) {
                            UIApplication.shared.open(deeplink)
                        }
                    },
                    openRawDataScreen: { type in
                        path.append(.settings(.rawData(type: type)))
                    },
                    openHistorySettings: {
                        path.append(.home(.locationHistorySettings(
                            groupId: GroupOrSpaceIdHelper.shared.unwrap(id: mapState.uiState.groupOrSpaceId)!
                        )))
                    },
                    openPaywall: {
                        path.append(.settings(.paywall(groupId: nil, type: BillingScreenSourceType.upgrade)))
                    },
                    onNavigateBack: {
                        path.removeLast()
                    },
                    mapUIView: { () -> UIViewController in
                        let controller = MapComponentVC()
                        controller.additionalSafeAreaInsets = .init(top: 65, left: 0, bottom: 0, right: 0)
                        controller.mapState = mapState
                        controller.showsTraffic = false
                        controller.onMapPan = {
                            mapState.vm.onEvent(event: LocationHistoryUiEventOnMapTouch())
                        }
                        controller.onMapLongPress = { point in
                            
                        }
                        return controller
                    }
                )
            }
            .ignoresSafeArea(.all, edges: .vertical)
            .toolbar(.hidden, for: .navigationBar)
        }
        .onAppear {
            Logr.d(tag:"history_screen", message: "History screen appear")
        }
        .onDisappear {
            Logr.d(tag:"history_screen", message: "History screen disappear")
            mapState.vm.onEvent(event: LocationHistoryUiEventUiLifecycleChange(active: false))
        }
        .onChange(of: scenePhase, initial: true) { oldScene, newScene in
            Logr.d(tag:"history_screen", message: "History screen scene switch (old: \(oldScene)) - (new: \(newScene))")
            
            switch newScene {
            case .active:
                mapState.vm.onEvent(event: LocationHistoryUiEventUiLifecycleChange(active: true))
            case .inactive:
                mapState.vm.onEvent(event: LocationHistoryUiEventUiLifecycleChange(active: false))
            case .background:
                mapState.vm.onEvent(event: LocationHistoryUiEventUiLifecycleChange(active: false))
            @unknown default:
                break
            }
        }
        .navigationBarBackButtonHidden(true)
    }
}
