//
//  PlacesScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-01-21.
//

import SwiftUI
import shared
import MapKit

struct PlacesScreen: View {
    
    @EnvironmentObject var appDelegate: AppDelegate
    @Binding var path: [NavigationGraph]
    @StateObject var appState: PlacesState
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.home.PlacesScreenVC(
                    vm: appState.vm,
                    addNewPlace: {
                        path.append(.home(.placeCreate(groupId: appState.groupId, placeId: nil, lat: nil, lng: nil)))
                    },
                    openPaywall: {
                        path.append(.settings(.paywall(type: BillingScreenSourceType.normal)))
                    },
                    openPlaceDetailScreen: { place in
                        path.append(.home(.placeDetail(groupId: place.groupId, placeId: place.id)))
                    },
                    askNotificationPermission: {
                        appDelegate.notificationPermissionHandler.requestNotificationPermission(
                            messageDispather: appState.vm.messageDispatcher
                        )
                    },
                    mapUIView: { state in
                        appState.updateMarkers(state: state)
                        let controller = MapComponentVC()
                        controller.mapState = appState
                        return controller
                    }
                )
            }
            .ignoresSafeArea(.all, edges: .bottom)
        }
        .toolbar {
            if appState.isOnboarding {
                ToolbarItem(placement: .navigationBarTrailing) {
                    if appState.markers.isEmpty {
                        SmallActionButtonView(
                            text: MR.shared.strings.button_skip.localizedString(),
                            action: {
                                path.append(.home(.sharingStart(groupId: appState.groupId, isOnboarding: appState.isOnboarding)))
                            },
                            hasEndArrow: true
                        )
                    } else {
                        SmallActionButtonView(
                            text: MR.shared.strings.button_next.localizedString(),
                            action: {
                                path.append(.home(.sharingStart(groupId: appState.groupId, isOnboarding: appState.isOnboarding)))
                            },
                            hasEndArrow: true
                        )
                    }
                }
            }
        }
        .toolbarTitleDisplayMode(.inline)
        .navigationTitle(MR.shared.strings.places_list_screen_title.localizedString())
        .navigationBarBackButtonHidden(appState.isOnboarding)
    }
    
    static func createState(groupId: String, isOnboarding: Bool) -> PlacesState {
        PlacesState(groupId: groupId, isOnboarding: isOnboarding)
    }
}

class PlacesState: BaseMapObservableObject {
    
    let groupId: String
    let isOnboarding: Bool
    let vm: PlacesViewModel
    
    init(groupId: String, isOnboarding: Bool) {
        self.groupId = groupId
        self.isOnboarding = isOnboarding
        self.vm = KoinIOS.ViewModels.shared.placesVM(groupId: groupId, isOnboarding: isOnboarding)
    }
    
    func updateMarkers(state: PlacesListScreenState) {
        markers = state.markers.map { marker in
            MapAnnotatedItem(marker: marker)
        }
        refocusMap(animate: false)
    }
    
    override func onMarkerTap(markerData: FocusedMarkerData) {
        refocusMap(animate: true)
    }
    
    deinit {
        vm.cleanup()
    }
}
