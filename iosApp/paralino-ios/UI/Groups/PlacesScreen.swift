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
                    addNewPlace: {},
                    openPlaceDetailScreen: { place in
                        path.append(.home(.placeDetail(groupId: place.groupId, placeId: place.id)))
                    },
                    onNavigateBack: {
                        path.removeLast()
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
                                    onMapTouch: {}
                                )
                            }
                        )
                    }
                )
            }
            .ignoresSafeArea(.all, edges: .bottom)
        }
    }
    
    static func createState(groupId: String) -> PlacesState {
        PlacesState(groupId: groupId)
    }
}

class PlacesState: BaseMapObservableObject {
    
    let groupId: String
    let vm: PlacesViewModel
    
    init(groupId: String) {
        self.groupId = groupId
        self.vm = KoinIOS.ViewModels.shared.placesVM(groupId: groupId)
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
