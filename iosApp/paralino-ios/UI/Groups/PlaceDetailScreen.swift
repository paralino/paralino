//
//  PlaceDetailScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-01-21.
//

import SwiftUI
import shared

struct PlaceDetailScreen: View {
    
    @EnvironmentObject var appDelegate: AppDelegate
    @Binding var path: [NavigationGraph]
    @StateObject var appState: PlaceDetailState
    
    @Environment(\.safeAreaInsets) private var safeAreaInsets
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.home.PlaceDetailScreenVC(
                    vm: appState.vm,
                    paddingValues: AppleScreenPaddingValues(
                        start: safeAreaInsets.start,
                        top: 0,
                        end: safeAreaInsets.end,
                        bottom: safeAreaInsets.bottom
                    ),
                    openPlaceRawDataScreen: {
                        path.append(.settings(.rawData(type: RawPresentationDataType.GroupPlacePresentation(placeId: appState.vm.placeId))))
                    },
                    openPlaceAlertsScreen: {
                        path.append(.home(.placeAlertsSettings(groupId: appState.groupId, placeId: appState.placeId)))
                    },
                    openPlaceSensitivityScreen: {
                        path.append(.home(.placeSensitivitySettings(groupId: appState.groupId, placeId: appState.placeId)))
                    },
                    openPlaceEditScreen: {
                        path.append(.home(.placeCreate(groupId: appState.groupId, placeId: appState.placeId, lat: nil, lng: nil)))
                    },
                    onNavigateBack: {
                        path.removeLast()
                    },
                    mapUIView: { state in
                        appState.updateMarkers(state: state)
                        let controller = MapComponentVC()
                        controller.mapState = appState
                        return controller
                    }
                )
            }
            .toolbarTitleDisplayMode(.inline)
            .ignoresSafeArea(.all, edges: .bottom)
            .navigationBarBackButtonHidden(false)
        }
    }
    
    static func createState(groupId: String, placeId: String) -> PlaceDetailState {
        PlaceDetailState(groupId: groupId, placeId: placeId)
    }
}

class PlaceDetailState: BaseMapObservableObject {
    
    let groupId: String
    let placeId: String
    let vm: PlaceDetailViewModel
    
    init(groupId: String, placeId: String) {
        self.groupId = groupId
        self.placeId = placeId
        self.vm = KoinIOS.ViewModels.shared.placeDetailVM(groupId: groupId, placeId: placeId)
    }
    
    func updateMarkers(state: PlaceDetailScreenState) {
        markers = [MapAnnotatedItem(marker: state.makeMapPin())]
        refocusMap(animate: false)
    }
    
    override func onMarkerTap(markerData: MapPinData) {
        focusToMarker(data: markerData.asFocusedMarker())
    }
    
    deinit {
        vm.cleanup()
    }
}

