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
    let notificationPermissionHandler = NotificationPermissionHandler()
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.home.PlacesScreenVC(
                    vm: appState.vm,
                    addNewPlace: {
                        path.append(.home(.placeCreate(groupId: appState.groupId, placeId: nil, lat: nil, lng: nil)))
                    },
                    openPlaceDetailScreen: { place in
                        path.append(.home(.placeDetail(groupId: place.groupId, placeId: place.id)))
                    },
                    askNotificationPermission: {
                        notificationPermissionHandler.requestNotificationPermission(appDelegate: appDelegate, messageDispather: appState.vm.messageDispatcher)
                    },
                    mapUIView: { state in
                        appState.updateMarkers(state: state)
                        return SwiftUIInUIView(
                            content: VStack {
                                MapComponent(
                                    mapState: appState,
                                    onMapTap: { point in },
                                    onMapLongPress: { point in },
                                    onMapTouch: {}
                                )
                            }
                        )
                    }
                )
            }
            .ignoresSafeArea(.all, edges: .bottom)
            .navigationTitle(appState.screenTitle)
            .navigationBarTitleDisplayMode(.inline)
        }
    }
    
    static func createState(groupId: String) -> PlacesState {
        PlacesState(groupId: groupId)
    }
}

class PlacesState: BaseMapObservableObject {
    
    let groupId: String
    let vm: PlacesViewModel
    @Published var screenTitle: String = ""
    
    init(groupId: String) {
        self.groupId = groupId
        self.vm = KoinIOS.ViewModels.shared.placesVM(groupId: groupId)
        
        super.init()
        
        self.vm.observe(flow: vm.uiState) { [weak self] state in
            if let uiState = PayloadSwift<PlacesListScreenState>(data: state).dataOrNil {
                self?.screenTitle = uiState.groupName
            }
        }
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
