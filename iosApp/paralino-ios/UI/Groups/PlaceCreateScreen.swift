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
    @State var bottomSheet: BottomSheetsScreen.EmojiPicker? = nil
    
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
                        bottomSheet = screen as? BottomSheetsScreen.EmojiPicker
                    },
                    mapUIView: {
                        return SwiftUIInUIView(
                            content: VStack {
                                MapComponent(
                                    mapState: appState,
                                    onMapTap: { point in },
                                    onMapLongPress: { point in appState.vm.updatePosition(lat: point.latitude, lng: point.longitude) },
                                    onMapTouch: {}
                                )
                            }
                        )
                    }
                )
            }
            .ignoresSafeArea(.keyboard, edges: .bottom)
            .navigationBarBackButtonHidden(false)
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
    
    let groupId: String
    let placeId: String?
    let lat: KotlinDouble?
    let lng: KotlinDouble?
    let vm: CreatePlaceViewModel
    
    init(groupId: String, placeId: String?, lat: Double? = nil, lng: Double? = nil) {
        self.groupId = groupId
        self.placeId = placeId
        self.lat = lat != nil ? KotlinDouble(value: lat!) : nil
        self.lng = lng != nil ? KotlinDouble(value: lng!) : nil
        self.vm = KoinIOS.ViewModels.shared.placeCreateVM(groupId: groupId, placeId: placeId, lat: self.lat, lng: self.lng)
        
        super.init()
        
        vm.observe(flow: vm.uiState) { [weak self] state in
            if let uiState = PayloadSwift<CreatePlaceScreenState>(data: state).dataOrNil {
                if uiState.lat != nil && uiState.lng != nil {
                    withAnimation {
                        self?.markers = [MapAnnotatedItem(marker: MapPinData(state: uiState))]
                    }
                }
                self?.refocusMap(animate: true, canInteruptAnimation: true)
            }
        }
    }
    
    override func onMarkerTap(markerData: FocusedMarkerData) {
        focusToMarker(data: markerData)
    }
    
    deinit {
        vm.cleanup()
    }
}
