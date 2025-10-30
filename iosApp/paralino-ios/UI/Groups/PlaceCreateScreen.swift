//
//  PlaceCreateScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-01-26.
//

import SwiftUI
import shared

struct PlaceCreateScreen: View {
    
    @EnvironmentObject var appDelegate: AppDelegate
    @Binding var path: [NavigationGraph]
    @StateObject var appState: PlaceCreateState
    @State var bottomSheet: BottomSheetPickerTypeEmojiPickerBottomSheet? = nil
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.home.PlaceCreateScreenVC(
                    vm: appState.vm,
                    onNavigateBack: {
                        path.removeLast()
                    },
                    onBottomSheetAction: { screen in
                        bottomSheet = screen
                    },
                    mapUIView: {
                        let controller = MapComponentVC()
                        controller.mapState = appState
                        controller.onMapTap = { point in
                            appState.vm.updatePosition(lat: point.latitude, lng: point.longitude)
                        }
                        controller.onMapLongPress = { point in
                            appState.vm.updatePosition(lat: point.latitude, lng: point.longitude)
                        }
                        return controller
                    }
                )
            }
            .ignoresSafeArea(.keyboard, edges: .bottom)
            .navigationTitle(appState.screenTitle)
            .navigationBarTitleDisplayMode(.inline)
            .highPriorityGesture(
                DragGesture(minimumDistance: 30, coordinateSpace: .local)
                    .onChanged { _ in /* no-op, just to claim */ }
                    .onEnded { _ in }
            )
        }
        
        .photoPickerSheet($bottomSheet) { image in
            appState.vm.updateImage(image: image)
            bottomSheet = nil
        }
    }
    
    static func createState(groupId: String, placeId: String?, lat: Double? = nil, lng: Double? = nil) -> PlaceCreateState {
        PlaceCreateState(groupId: groupId, placeId: placeId, lat: lat, lng: lng)
    }
}

class PlaceCreateState: BaseMapObservableObject {
    
    let vm: CreatePlaceViewModel
    @Published var isNewPlace: Bool? = nil
    @Published var screenTitle: String = ""
    
    init(groupId: String, placeId: String?, lat: Double? = nil, lng: Double? = nil) {
        
        if placeId == nil {
            screenTitle = MR.shared.strings.places_create_screen_title.localizedString()
        } else {
            screenTitle = MR.shared.strings.places_edit_screen_title.localizedString()
        }
        
        let lat = lat != nil ? KotlinDouble(value: lat!) : nil
        let lng = lng != nil ? KotlinDouble(value: lng!) : nil
        self.vm = KoinIOS.ViewModels.shared.placeCreateVM(groupId: groupId, placeId: placeId, lat: lat, lng: lng)
        
        super.init()
        
        vm.observe(flow: vm.uiState) { [weak self] state in
            if let uiState = PayloadSwift<CreatePlaceScreenState>(data: state).dataOrNil {
                if uiState.lat != nil && uiState.lng != nil {
                    withAnimation {
                        self?.markers = [MapAnnotatedItem(marker: MapPinData(state: uiState))]
                    }
                }
                self?.refocusMap(animate: true, canInteruptAnimation: true)
                self?.isNewPlace = uiState.isNewPlace
                if uiState.isNewPlace {
                    self?.screenTitle = MR.shared.strings.places_create_screen_title.localizedString()
                } else {
                    self?.screenTitle = MR.shared.strings.places_edit_screen_title.localizedString()
                }
            }
        }
    }
    
    override func onMarkerTap(markerData: MapPinData) {
        focusToMarker(data: markerData.asFocusedMarker())
    }
    
    deinit {
        vm.cleanup()
    }
}
