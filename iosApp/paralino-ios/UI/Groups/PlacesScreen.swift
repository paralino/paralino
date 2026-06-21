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
    
    @Environment(\.safeAreaInsets) private var safeAreaInsets
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.home.PlacesScreenVC(
                    vm: appState.vm,
                    paddingValues: AppleScreenPaddingValues(
                        start: safeAreaInsets.start,
                        top: 0,
                        end: safeAreaInsets.end,
                        bottom: safeAreaInsets.bottom
                    ),
                    addNewPlace: {
                        path.append(.home(.placeCreate(groupId: appState.groupId, placeId: nil, editStep: nil, lat: nil, lng: nil)))
                    },
                    openPaywall: {
                        path.append(.settings(.paywall(type: BillingScreenSourceType.upgradeDefault)))
                    },
                    onSkipOnboarding: {
                        path.append(.home(.sharingStart(groupId: appState.groupId, isOnboarding: appState.isOnboarding)))
                    },
                    openPlaceDetailScreen: { place in
                        path.append(.home(.placeDetail(groupId: GroupOrSpaceIdHelper.shared.unwrap(id: place.groupOrSpaceId)!, placeId: place.id)))
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
    
    override func onMarkerTap(markerData: MapPinData) {
        refocusMap(animate: true)
    }
    
    deinit {
        vm.cleanup()
    }
}
