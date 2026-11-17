//
//  PrivacyZoneCreateScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 11.06.26.
//

import SwiftUI
import shared
import MapKit

struct PrivacyZoneCreateScreen: View {

    @Binding var path: [NavigationGraph]
    @StateObject var appState: CreatePrivacyZoneState
    @State var bottomSheet: BottomSheetPickerTypeEmojiPickerBottomSheet? = nil

    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.home.PrivacyZoneCreateScreenVC(
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
                            appState.vm.onEvent(event: CreatePrivacyZoneScreenUiEventOnMapTap(lat: point.latitude, lng: point.longitude))
                        }
                        controller.onMapLongPress = { point in
                            appState.vm.onEvent(event: CreatePrivacyZoneScreenUiEventOnMapTap(lat: point.latitude, lng: point.longitude))
                        }
                        return controller
                    }
                )
            }
            .navigationTitle(appState.screenTitle)
            .navigationBarTitleDisplayMode(.inline)
            .highPriorityGesture(
                DragGesture(minimumDistance: 30, coordinateSpace: .local)
                    .onChanged { _ in }
                    .onEnded { _ in }
            )
        }
        .photoPickerSheet($bottomSheet) { image in
            appState.vm.onEvent(event: CreatePrivacyZoneScreenUiEventOnImageChanged(image: image))
            bottomSheet = nil
        }
    }

    static func createState(groupId: String, zoneId: String?, editStep: CreatePrivacyZoneScreenStep?, lat: Double? = nil, lng: Double? = nil) -> CreatePrivacyZoneState {
        CreatePrivacyZoneState(groupId: groupId, zoneId: zoneId, editStep: editStep, lat: lat, lng: lng)
    }
}

class CreatePrivacyZoneState: BaseMapObservableObject {

    // Read by the nonisolated deinit to clean up the view model.
    nonisolated(unsafe) let vm: CreatePrivacyZoneViewModel
    @Published var screenTitle: String = ""

    init(groupId: String, zoneId: String?, editStep: CreatePrivacyZoneScreenStep?, lat: Double? = nil, lng: Double? = nil) {
        if zoneId == nil {
            screenTitle = MR.shared.strings.privacy_zones_create_screen_title.localizedString()
        } else {
            screenTitle = MR.shared.strings.privacy_zones_edit_screen_title.localizedString()
        }

        let lat = lat != nil ? KotlinDouble(value: lat!) : nil
        let lng = lng != nil ? KotlinDouble(value: lng!) : nil
        self.vm = KoinIOS.ViewModels.shared.privacyZoneCreateVM(groupId: groupId, zoneId: zoneId, editStep: editStep, lat: lat, lng: lng)

        super.init()

        vm.observe(flow: vm.uiState) { [weak self] state in
            if let uiState = PayloadSwift<CreatePrivacyZoneScreenState>(data: state).dataOrNil {
                if uiState.lat != nil && uiState.lng != nil {
                    withAnimation {
                        self?.markers = [MapAnnotatedItem(marker: uiState.makeMapPin())]
                    }
                }
                self?.refocusMap(animate: true, canInteruptAnimation: true)
                if uiState.isNewZone {
                    self?.screenTitle = MR.shared.strings.privacy_zones_create_screen_title.localizedString()
                } else {
                    self?.screenTitle = MR.shared.strings.privacy_zones_edit_screen_title.localizedString()
                }
                self?.isSatelliteMode = uiState.isSatelliteModeActive
            }
        }
    }

    override func onMarkerTap(markerData: MapPinData) {
        focusToMarker(data: markerData.asFocusedMarker())
    }

    override func regionFinishedUpdating() {
        super.regionFinishedUpdating()
        if let position = cameraPosition.cameraRect {
            let region = MKCoordinateRegion.init(position).center
            vm.onEvent(event: CreatePrivacyZoneScreenUiEventOnMapPositionChanged(position: LatLng(lat: region.latitude, lng: region.longitude)))
        }
    }

    deinit {
        vm.cleanup()
    }
}
