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
        markers = [MapAnnotatedItem(marker: MapPinData(state_: state))]
        refocusMap(animate: false)
    }
    
    override func onMarkerTap(markerData: FocusedMarkerData) {
        focusToMarker(data: markerData)
    }
    
    deinit {
        vm.cleanup()
    }
}

