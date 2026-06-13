//
//  PrivacyZoneDetailScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 11.06.26.
//

import SwiftUI
import shared

struct PrivacyZoneDetailScreen: View {

    @Binding var path: [NavigationGraph]
    @StateObject var appState: PrivacyZoneDetailState

    @Environment(\.safeAreaInsets) private var safeAreaInsets

    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.home.PrivacyZoneDetailScreenVC(
                    vm: appState.vm,
                    paddingValues: AppleScreenPaddingValues(
                        start: safeAreaInsets.start,
                        top: 0,
                        end: safeAreaInsets.end,
                        bottom: safeAreaInsets.bottom
                    ),
                    openPrivacyZoneRawDataScreen: {
                        path.append(.settings(.rawData(type: RawPresentationDataType.GroupPrivacyZonePresentation(privacyZoneId: appState.vm.zoneId))))
                    },
                    openPrivacyZoneNameEditScreen: {
                        path.append(.home(.privacyZoneCreate(groupId: appState.groupId, zoneId: appState.zoneId, editStep: CreatePrivacyZoneScreenStep.nameSuggestion, lat: nil, lng: nil)))
                    },
                    openPrivacyZonePositionEditScreen: {
                        path.append(.home(.privacyZoneCreate(groupId: appState.groupId, zoneId: appState.zoneId, editStep: CreatePrivacyZoneScreenStep.address, lat: nil, lng: nil)))
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

    static func createState(groupId: String, zoneId: String) -> PrivacyZoneDetailState {
        PrivacyZoneDetailState(groupId: groupId, zoneId: zoneId)
    }
}

class PrivacyZoneDetailState: BaseMapObservableObject {

    let groupId: String
    let zoneId: String
    let vm: PrivacyZoneDetailViewModel

    init(groupId: String, zoneId: String) {
        self.groupId = groupId
        self.zoneId = zoneId
        self.vm = KoinIOS.ViewModels.shared.privacyZoneDetailVM(groupId: groupId, zoneId: zoneId)
    }

    func updateMarkers(state: PrivacyZoneDetailScreenState) {
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
